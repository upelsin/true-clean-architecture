package com.example.clean.screens

import com.example.clean.R
import com.example.domain.submitProposal.SubmitProposal

data class ProposalConfirmationScreen(
        override val state: SubmitProposal.ViewState,
        override val events: Events
) : Screen {
    override val listBindings: ListBindings
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val layout: Int
        get() = R.layout.proposal_confirmation_view

    data class Events(
            val coverLetter: CoverLetterScreenEvents,
            val clarifyingQuestions: ClarifyingQuestionsEvents,
            val doSubmitProposal: DoSubmitProposalEvents
    ) : ScreenEvents


}