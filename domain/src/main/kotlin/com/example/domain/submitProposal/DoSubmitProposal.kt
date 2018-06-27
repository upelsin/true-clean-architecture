package com.example.domain.submitProposal

import com.example.domain.UiCommand
import com.example.domain.UiResult
import com.example.domain.models.Proposal
import io.reactivex.Observable
import io.reactivex.ObservableTransformer

sealed class DoSubmitProposalCommand : UiCommand {
    data class DoSubmit(val proposal: Proposal) : DoSubmitProposalCommand()
    object Remove : DoSubmitProposalCommand()
}

sealed class DoSubmitProposalResult : UiResult {
    object InProgress : DoSubmitProposalResult()
    //    object ProposalRemoved : DoSubmitProposalResult()
    data class Success(val response: String) : DoSubmitProposalResult()
    data class Error(val exception: Throwable) : DoSubmitProposalResult()
}

//handle job updates to update job title in Proposal, and questions
//alert if both changed?

interface Api {
    fun submitProposal(id: String, some: String): Observable<String>
}

val doSubmitProposalProcessor =
        ObservableTransformer<DoSubmitProposalCommand, UiResult> {
            it.flatMap {
                when (it) {
                    is DoSubmitProposalCommand.DoSubmit -> {
                        val api: Api? = null

                        api!!.submitProposal("123", "dsf")
                                .map(DoSubmitProposalResult::Success)
                                .cast(UiResult::class.java)
                                .onErrorReturn(DoSubmitProposalResult::Error)
                                .startWith(DoSubmitProposalResult.InProgress)
                    }
                    DoSubmitProposalCommand.Remove -> {
                        Observable.just<UiResult>(DoSubmitProposalResult.InProgress)
                    }
                }
            }

        }
