package com.example.domain.submitProposal2.doSubmitProposal.proposalConfirmation

import com.example.domain.UiCommand
import com.example.domain.UiResult
import com.example.domain.UiState
import com.example.domain.framework.ExtraCommandsComponent
import com.example.domain.framework.Navigator
import io.reactivex.Observable
import io.reactivex.ObservableTransformer

class ProposalConfirmation(
        navigator: Navigator
) : ExtraCommandsComponent<ProposalConfirmation.Command, ProposalConfirmation.Result, ProposalConfirmation.ViewState>() {
    val pcProcessor =
            ObservableTransformer<Command, Result> {
                it.map {
                    when (it) {
                        is Command.DATA -> Result.DATALoaded(it.itemOpportunity)
                        Command.Dismiss -> Result.Dismissed
                        Command.AnotherOne -> {
                            navigator.display(
                                    DummyConfirmation(),
                                    DummyConfirmation.Command.DATA("some dummy"))
                            Result.Displayed
                        }
                    }
                }
            }

    val pcReducer =
            ObservableTransformer<Result, ViewState> {
                it.flatMap {
                    when (it) {
                        is Result.DATALoaded -> Observable.just(ViewState(it.itemOpportunity))
                        is Result.Dismissed -> {
                            println("DISMISSED")
                            Observable.empty()
                        }
                        Result.Displayed -> Observable.just(ViewState("whoa"))
                    }
                }
            }

    override val processor = pcProcessor
    override val reducer = pcReducer


    sealed class Command : UiCommand {
        data class DATA(val itemOpportunity: String) : Command()

        object AnotherOne : Command()

        object Dismiss : Command()
    }

    sealed class Result : UiResult {
        data class DATALoaded(val itemOpportunity: String) : Result()

        object Dismissed : Result()
        object Displayed : Result()
    }

    data class ViewState(
            val title: String
    ) : UiState
}
