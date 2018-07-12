package com.example.domain.submitProposal


import com.example.domain.UiCommand
import com.example.domain.UiComponent
import com.example.domain.UiResult
import com.example.domain.UiState
import com.example.domain.models.ItemDetails
import com.example.domain.models.ItemOpportunity
import com.example.domain.submitProposal.SubmitProposal.Command
import com.example.domain.submitProposal.SubmitProposal.ViewState
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.ReplaySubject


class SubmitProposal(
        val coverLetter: CoverLetter,
        val clarifyingQuestions: ClarifyingQuestions,
        val doSubmitProposal: DoSubmitProposal
) : UiComponent<Command, UiResult, ViewState> {

    val cmd = PublishSubject.create<UiCommand>()

    fun fromEvent(command: SubmitProposal.Command) {
        cmd.onNext(command)
    }

    lateinit var results: Observable<UiResult>

    val loopbackCommands = ReplaySubject.create<UiCommand>()

    override fun process(commands: Observable<Command>): Observable<UiResult> {
        val c = commands
                .doOnNext { println("CMDSP " + it) }
                //.takeUntil
                .cast(UiCommand::class.java)
                .mergeWith(cmd)
                .mergeWith(loopbackCommands.doOnNext { println("GOT LB $it") })
                .doOnNext { println("RESSP " + it) }
                .publish<UiResult> { shared ->
                    Observable.merge<UiResult>(
                            Observable.just(

                                    shared.ofType(SubmitProposal.Command::class.java)
                                            .compose(storageLoader),
                                    shared.ofType(SubmitProposal.Command.ToNextStep::class.java)
                                            .compose(navigationProcessor),

                                    shared.ofType(CoverLetter.Command::class.java)
                                            .compose { coverLetter.process(it) },
                                    shared.ofType(ClarifyingQuestions.Command::class.java)
                                            .compose { clarifyingQuestions.process(it) },

                                    shared.ofType(DoSubmitProposal.Command::class.java)
                                            .compose { doSubmitProposal.process(it) }

                            )
                            /*,


                            shared.ofType(FeesCommands::class.java)
                                    .compose(feesProc)*/
                    )
                }
                .publish { shared ->
                    Observable.merge(
                            shared,
                            shared.compose(submitAllowedProcessor),//.doOnNext { println(">>>> SAP: $it") },
                            shared.compose(storageSaver)
                    )
                }
                .publish { shared ->
                    Observable.merge(
                            shared,//.doOnNext { println(">>>> SHARED AFTER SAP: $it") },
                            shared.compose(submitProposalResultsProcessor)
                    )
                }//.doOnNext { println(">>>> BEFORE SHARE: $it") }
                //.startWith(Result.Dummy)
                //.share()
                //.publish().autoConnect(2).doOnNext { println(">>>> AFTER SHARE: $it") }
                .replay().refCount().doOnNext { println(">>>> AFTER SHARE: $it") }


        //properly unsubscribe?
        c.compose(fromResultToCommands)
                //.takeWhile
                .doOnNext { println("GOT COMMAND: $it") }
                //.delay(10, TimeUnit.MILLISECONDS)
                .subscribe {
                    println("GOT COMMAND SUB $it")
                    loopbackCommands.onNext(it)
                }

        results = c

        return c.doOnNext { println(">>>> C: $it") }
        // .cast(Result::class.java)
    }

    override fun render(): Observable<ViewState> {
        return Observable.combineLatest(
                arrayOf(
                        coverLetter.render().doOnNext { println("render CL: $it") },
                        clarifyingQuestions.render().doOnNext { println("render CQ: $it") },

                        results.compose(psReducer),

                        results.ofType(SubmitProposal.Result.NavigatedTo::class.java)
                                .map {
                                    println(">>>>>><<<<<<<< index ${it.index}")
                                    it.index
                                },

                        doSubmitProposal.render().doOnNext { println("render DSP: $it") }
                )
        ) {
            val cl = it[0] as CoverLetter.ViewState
            val cq = it[1] as ClarifyingQuestions.ViewState
            val ps = it[2] as ProposalSummaryViewState
            val index = it[3] as Int
            val dsp = it[4] as DoSubmitProposal.ViewState
            ViewState(cl, cq, ps, dsp, index)
        }
    }

    sealed class Command : UiCommand {

        data class DATA(val itemDetails: ItemDetails) : Command()

        object ToNextStep : Command()
        object ToCoverLetter : Command()
        object ToClarifyingQuestions : Command()

        object RemoveProposal : Command() //when job became private
    }

    val fromResultToCommands =
            ObservableTransformer<UiResult, UiCommand> {
                it.flatMap {

                    //memoize itemOpportunity here as well?
                    when (it) {
                        is Result.ProposalUpdated -> {
                            println("112233 PROPOSAL UPDATED")
                            Observable.empty<UiCommand>()
                        }
                        is SubmitAllowedResult.Enabled -> {
                            //Observable.just(ProposalSummary.Command.ToggleSubmitEnabled(true))
                            println("SUBMIT ENABLED!")

                            Observable.just(DoSubmitProposal.Command.ToggleSubmitEnabled(true))
                        }

                        is Result.ProposalLoaded -> {
                            //hide panel
                            Observable.fromArray(
                                    CoverLetter.Command.DATA(it.itemOpportunity!!),
                                    ClarifyingQuestions.Command.INIT(it.itemOpportunity),
                                    DoSubmitProposal.Command.DATA(it.itemOpportunity)
                            )
                            //AnchoredPanel.Command.Expand

                        }
                        is Result.ProposalRemoved -> {
                            //hide panel

                            Observable.empty<UiCommand>()

                        }
                        is FeesResult.CalculatorLoaded -> {
                            //ProposeTerms.Command.DATA
                            Observable.empty<UiCommand>()
                        }
                        is DoSubmitProposal.Result.Success -> {
                            println("SUCCESS")
                            Observable.just(SubmitProposal.Command.RemoveProposal)
                        }

                        else -> {
                            Observable.empty<UiCommand>()
                        }
                    /*is SuggestedRateResult.SuggestAccepted -> {
                        ProposeTermsCommands.UpdateBid(it.suggestedRate)
                    }
                    is FeesResult.CalculatorLoaded -> {
                        //and navigate to it
                        ProposeTermsCommands.RecalculateBidEarn
                    }
                    is AnchorablePanelResult.Discard -> {
                        //show dialog first
                        //hide panel
                        DoSubmitProposalCommand.Remove
                    }


                    is AnchorablePanelResult.PageChanged -> {
                        //show dialog first
                        DoSubmitProposalCommand.Expand
                    }
                    is DoSubmitProposalResult.Success -> {
                        DoSubmitProposalCommand.Hide
                    }
                    is DoSubmitProposalResult.Error -> {
                        DoSubmitProposalCommand.Hide
                    }*/
                    /*ProposalSummaryEvents.OnSubmitProposal -> {
                        SubmitProposalCommand.DoSubmit(proposal)
                    }

                    ProposalSummaryEvents.ToCoverLetter -> {
                        AnchorablePanelCommands.NavigateTo.SpecificPage(1)

                    }
                    ProposalSummaryEvents.ToPineappleQuestions -> {
                        AnchorablePanelCommands.NavigateTo.SpecificPage(2)
                    }
                    ProposalSummaryEvents.ToProposeTerms -> {
                        AnchorablePanelCommands.NavigateTo.SpecificPage(0)
                    }*/
                    }
                }
            }

    val submitProposalResultsProcessor =
            ObservableTransformer<UiResult, Result> {
                it.flatMap {
                    when (it) {
                        CoverLetter.Result.Empty,
                        is CoverLetter.Result.Valid,
                        is ClarifyingQuestions.Result.ValidAnswer,
                        is ClarifyingQuestions.Result.EmptyAnswer,
                        is ProposeTerms.Result.BidValid,
                        ProposeTerms.Result.BidEmpty ->//,
                            //is ProposeTerms.Result.EngagementSelected ->

                            Observable.just(Result.ProposalUpdated)

                        is DoSubmitProposal.Result.Success ->
                            Observable.just(Result.ProposalSent)

                        is DoSubmitProposal.Result.Error ->
                            Observable.just(Result.JobNoLongerAvailable)

                        else -> Observable.empty()
                    }
                }
            }

    sealed class Result : UiResult {
        object ProposalRemoved : Result()
        object ProposalSent : Result() //change to already applied

        object ProposalUpdated : Result()
        data class ProposalLoaded(val itemOpportunity: ItemOpportunity) : Result() //time

        object JobIsPrivate : Result()
        object JobNoLongerAvailable : Result()


        data class NavigatedTo(val index: Int) : Result()

        object Dummy : Result()
    }

    data class ViewState(
            val coverLetter: CoverLetter.ViewState,
            val clarifyingQuestions: ClarifyingQuestions.ViewState,
            val proposalSummary: ProposalSummaryViewState,
            val doSubmitProposal: DoSubmitProposal.ViewState,
            val index: Int
    ) : UiState {
        companion object {
            /*fun initial() = SubmitProposalViewState(
                    CoverLetterViewState("2312", false),
                    PineappleQuestionsViewState(listOf())
            )*/
        }
    }
}
