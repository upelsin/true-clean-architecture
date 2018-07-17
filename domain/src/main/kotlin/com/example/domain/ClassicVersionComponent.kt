package com.example.domain

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.subjects.PublishSubject

abstract class ClassicVersionComponent<C : UiCommand, R : UiResult, S : UiState> : Actor<C, R>, UiRenderer<S> {

    private val commands = PublishSubject.create<C>()

    protected abstract val processor: ObservableTransformer<C, R>

    protected abstract val reducer: ObservableTransformer<R, S>

    private lateinit var results: Observable<R>

    override fun apply(upstream: Observable<C>): ObservableSource<R> {
        println("123")
/*
        check(::results.isInitialized) {
            "Can't compose component more than once"
        }
*/println("set apply")

        val up = upstream.publish().refCount()

        val transformed = upstream.doOnComplete { println("upstream complete") }.doOnDispose { println("upstream dispose") }
                .mergeWith(commands.doOnComplete { println("subj completed") }.doOnDispose { println("subj disposed") })
                .compose(processor)
                .doOnComplete { println("complete") }
                .doOnDispose { println("dispose") }
                .doOnLifecycle({ println("onSub")}, { println("onDisp")})
                .replay()
                .refCount()
                .doOnComplete { println("complete2") }
                .doOnDispose { println("dispose2") }
                //maybe two lines up
                //.takeUntil(upstream.materialize().filter { it.isOnComplete })
                .doOnComplete { println("complete3") }
                .doOnDispose { println("dispose3") }
                .doOnLifecycle({ println("onSub3")}, { println("onDisp3")})

        results = transformed
        println("set results")

        return transformed.doOnNext { println("trans $it") }
                //.takeUntil(up.materialize().doOnNext { println("MAT $it") }.filter { it.isOnComplete })
                .doOnComplete { println("trans compl") }
    }

    override fun render(): Observable<S> {
        checkNotNull(results) {
            "Render() must be called only after composing component"
        }

        return results.compose(reducer)
    }

    fun sendCommand(command: C) {
        check(::results.isInitialized) {
            "Can't compose component more than once"
        }


        println("send command")
        commands.onNext(command)
    }

    fun justStart(command: C) {
        Observable.just(command).compose(this).subscribe()
    }

    fun standalone(command: C) {
        Observable.just(command).compose(this).firstOrError().subscribe()
    }
}
