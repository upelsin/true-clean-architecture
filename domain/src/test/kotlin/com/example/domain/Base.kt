package com.example.domain

import io.reactivex.observers.TestObserver
import io.reactivex.subjects.ReplaySubject
import org.jetbrains.spek.api.dsl.SpecBody
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.lifecycle.CachingMode


@TestDsl
fun <C: UiCommand, R: UiResult, T: UiActor<C, R>> SpecBody.component(
        componentCreator: () -> T,
        body: Component<C, R, T>.() -> Unit
) =
        Component(this, componentCreator).apply {
            context("a component ...") {
                body()
            }
        }


@TestDsl
fun <C: UiCommand, R: UiResult, T: UiActor<C, R>> Component<C, R, T>.initialized(
        cmd: C,
        body: InitializedComponent<C, R, T>.() -> Unit
) =
        InitializedComponent(cmd, this).apply(body)


data class Deps<C : UiCommand, R : UiResult>(
        val cmdstr: ReplaySubject<C>,
        val sub: TestObserver<R>,
        val component: UiActor<C, R>
)

@TestDsl
open class Component<C: UiCommand, R: UiResult, out T : UiActor<C, R>>(
        val specBody: SpecBody,
        val componentCreator: () -> T
) {

    /*protected*/ open val deps by specBody.memoized(CachingMode.GROUP) {

        println("deps")
        val cmdstr =
            ReplaySubject.create<C>()

        val component = componentCreator()
        val sub =
            component.process(cmdstr).test()

        Deps(cmdstr, sub, component)
    }

    infix fun command(command: C) {
        println("command")
        val cmdstr = deps.cmdstr
        println("after init")
        cmdstr.onNext(command)
    }

    fun assertResultAt(index: Int, result: R): TestObserver<out R> {
        println("assert $result")

        return deps.sub.assertValueAt(index, result)
    }

    fun assertValuesOnly(vararg values: R) = deps.sub.assertValuesOnly(*values)
}

@TestDsl
class InitializedComponent<C: UiCommand, R: UiResult, out T: UiActor<C, R>>(
        cmd: C,
        component: Component<C, R, T>
) : Component<C, R, T>(component.specBody, component.componentCreator) {

    override val deps by specBody.memoized(CachingMode.GROUP) {
        val d = super.deps
        d.cmdstr.onNext(cmd)
        d
    }

    fun furtherValueOnly(value: R) =
            deps.sub.assertSubscribed()
                    .assertValueAt(1, value)
                    .assertNoErrors()
                    .assertNotComplete()
}

fun SpecBody.perform(callback: () -> Unit) = this.beforeEachTest(callback)
fun SpecBody.once(callback: () -> Unit) = this.beforeGroup(callback)
