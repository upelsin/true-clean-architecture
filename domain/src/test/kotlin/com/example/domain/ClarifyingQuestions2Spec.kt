package com.example.domain

import com.example.domain.models.ItemDetails
import com.example.domain.models.ItemOpportunity
import com.example.domain.models.Proposal
import com.example.domain.models.Question
import com.example.domain.submitProposal.ClarifyingQuestions
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

//@RunWith(JUnitPlatform::class)
class ClarifyingQuestions2Spec : Spek({

    val itemOpportunity = ItemOpportunity(
            ItemDetails("1234", null),
            Proposal(0, null)
    )

    val questions = listOf(Question("1", "q1"), Question("2", "q1"))
    val itemOpportunity2 = ItemOpportunity(
            ItemDetails("1234", questions),
            Proposal(0, null)
    )

    group("sdaf") {

        component(::ClarifyingQuestions) {

            initialized(ClarifyingQuestions.Command.INIT(itemOpportunity2)) {

                on("init2") {
                    it("should2") {
                        assertResultAt(0, ClarifyingQuestions.Result.Questions(questions))
                    }
                }

                on("init3") {
                    it("should3") {
                        assertResultAt(0, ClarifyingQuestions.Result.Questions(questions))
                    }
                }


            }
        }
        component(::ClarifyingQuestions) {

            initialized(ClarifyingQuestions.Command.INIT(itemOpportunity)) {

                on("init") {
                    it("should") {
                        assertResultAt(0, ClarifyingQuestions.Result.NoQuestions)
                    }
                }


            }


        }
    }
})