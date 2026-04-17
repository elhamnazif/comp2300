package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.model.education.QuizQuestion
import com.group8.comp2300.domain.model.education.QuizOption

val saferSexQuiz = Quiz(
    id = "q1-safer-sex-basics",
    articleId = "safer-sex-basics",
    title = "Safer Sex Basics",
    questions = listOf(
        QuizQuestion(
            id = "q-ssb-1",
            title = "What is the primary definition of 'safer sex' according to the text?",
            explanation = "Safer sex involves specific actions, such as using barriers and getting tested, to lower the risk of spreading infections (STDs/STIs) during vaginal, anal, and oral activities.",
            options = listOf(
                QuizOption("opt-ss1a", "Sex that is guaranteed to prevent pregnancy.", false),
                QuizOption("opt-ss1b", "Taking steps to protect yourself and your partner from STDs.", true),
                QuizOption("opt-ss1c", "Only having sex with one person for your entire life.", false),
                QuizOption("opt-ss1d", "Using birth control pills every single day.", false)
            )
        ),
        QuizQuestion(
            id = "q-ssb-2",
            title = "Which of the following is an effective barrier to use during oral sex to reduce the risk of STDs?",
            explanation = "Dental dams are thin squares made of latex or nitrile that act as a physical shield between the mouth and genitals, preventing the exchange of fluids and direct skin contact.",
            options = listOf(
                QuizOption("opt-ss2a", "Lambskin condoms.", false),
                QuizOption("opt-ss2b", "Oil-based lubricants.", false),
                QuizOption("opt-ss2c", "Dental dams.", true),
                QuizOption("opt-ss2d", "Hormonal birth control.", false)
            )
        ),
        QuizQuestion(
            id = "q-ssb-3",
            title = "Why is regular STD testing necessary even if you feel healthy and have no symptoms?",
            explanation = "Many STDs are 'asymptomatic,' meaning infected individuals can feel perfectly fine while still being able to pass the infection. Testing is the only definitive way to confirm health status.",
            options = listOf(
                QuizOption("opt-ss3a", "Because condoms are only 10% effective.", false),
                QuizOption("opt-ss3b", "Because most people with STDs do not have symptoms but can still pass the infection.", true),
                QuizOption("opt-ss3c", "Because testing is the only way to physically block an infection.", false),
                QuizOption("opt-ss3d", "Because you can only get an STD if you feel sick.", false)
            )
        ),
        QuizQuestion(
            id = "q-ssb-4",
            title = "Which of these STDs can be spread through skin-to-skin contact alone, even if no sexual fluids are exchanged?",
            explanation = "While fluid-based STDs like HIV require semen or blood, skin-to-skin STDs like HPV, Herpes, and Syphilis live on the surface of the genital area and can pass through simple contact.",
            options = listOf(
                QuizOption("opt-ss4a", "HIV and Hepatitis B.", false),
                QuizOption("opt-ss4b", "Chlamydia and Gonorrhea.", false),
                QuizOption("opt-ss4c", "HPV, Herpes, and Syphilis.", true),
                QuizOption("opt-ss4d", "None; all STDs require fluid exchange.", false)
            )
        ),
        QuizQuestion(
            id = "q-ssb-5",
            title = "Which activity is categorized as 'risk-free' in terms of STD transmission?",
            explanation = "Masturbating alone involves zero contact with others and no exchange of skin or fluids. Other activities like oral sex are 'lower risk' but not entirely risk-free.",
            options = listOf(
                QuizOption("opt-ss5a", "Oral sex with a condom.", false),
                QuizOption("opt-ss5b", "Dry humping without clothes.", false),
                QuizOption("opt-ss5c", "Masturbating alone.", true),
                QuizOption("opt-ss5d", "Sharing sex toys after washing them.", false)
            )
        ),
        QuizQuestion(
            id = "q-ssb-6",
            title = "If you are diagnosed with a curable STD (like Chlamydia or Gonorrhea), what is the correct protocol for safer sex?",
            explanation = "Curable STDs stay in the body until the full treatment is finished. Partners must be treated simultaneously to prevent re-infection, and sex should be avoided until treatment is complete.",
            options = listOf(
                QuizOption("opt-ss6a", "Stop taking medication as soon as the itching or pain stops.", false),
                QuizOption("opt-ss6b", "Resume sex immediately after taking the first pill.", false),
                QuizOption("opt-ss6c", "Finish all medication and ensure your partner is also treated before having sex again.", true),
                QuizOption("opt-ss6d", "Switch to 'outercourse' only for 24 hours.", false)
            )
        )
    )
)

val testingQuiz = Quiz(
    id = "q-getting-tested-for-stds",
    articleId = "getting-tested-for-stds",
    title = "Getting Tested For STDs",
    questions = listOf(
        QuizQuestion(
            id = "q-teststd-1",
            title = "Which of the following is the most accurate reason to get an STD test?",
            explanation = "Many STDs show no symptoms at all. The only way to know if you have one is to get tested after potential exposure or when starting a relationship with a new partner.",
            options = listOf(
                QuizOption("opt-t1a", "You only need a test if you notice a physical change, like a sore.", false),
                QuizOption("opt-t1b", "You should get tested if you have had any kind of unprotected sexual contact.", true),
                QuizOption("opt-t1c", "You are automatically tested for all STDs during a regular physical exam.", false),
                QuizOption("opt-t1d", "Testing is only necessary if your partner is showing symptoms.", false)
            )
        ),
        QuizQuestion(
            id = "q-teststd-2",
            title = "How are samples typically collected for common STD tests like Chlamydia or Gonorrhea?",
            explanation = "Most common STDs are detected through simple samples like urine or swabs. Swabs may be taken from the vagina, penis, throat, or anus depending on the type of sex you have.",
            options = listOf(
                QuizOption("opt-t2a", "Through a standard X-ray or ultrasound.", false),
                QuizOption("opt-t2b", "By checking your temperature and blood pressure.", false),
                QuizOption("opt-t2c", "By using a quick urine sample or a gentle swab of the genital area.", true),
                QuizOption("opt-t2d", "By taking a small hair or fingernail sample.", false)
            )
        ),
        QuizQuestion(
            id = "q-teststd-3",
            title = "Why is it important to be honest with a health provider about the 'type' of sex you have (oral, anal, or vaginal)?",
            explanation = "A urine test might miss an infection that is only present in the throat or rectum. Being honest ensures the doctor tests every area that might have been exposed.",
            options = listOf(
                QuizOption("opt-t3a", "So the provider can determine if you need swabs in specific areas.", true),
                QuizOption("opt-t3b", "Because some STDs can only be found in the blood.", false),
                QuizOption("opt-t3c", "To ensure you are charged the correct amount for the visit.", false),
                QuizOption("opt-t3d", "It isn’t important; a urine test finds every infection in the body.", false)
            )
        ),
        QuizQuestion(
            id = "q-teststd-4",
            title = "What is the 'window period' in STD testing?",
            explanation = "It can take anywhere from a few days to a few months for an infection to trigger a positive test result. Testing too early (immediately after sex) might result in a 'false negative.'",
            options = listOf(
                QuizOption("opt-t4a", "The amount of time you have to wait in the clinic for results.", false),
                QuizOption("opt-t4b", "The time it takes for an infection to show up on a medical test.", true),
                QuizOption("opt-t4c", "The time between your first and second dose of medication.", false),
                QuizOption("opt-t4d", "The 24-hour period where you are most contagious.", false)
            )
        ),
        QuizQuestion(
            id = "q-teststd-5",
            title = "What happens if an STD test comes back positive for a bacterial infection like Syphilis?",
            explanation = "Bacterial STDs like Syphilis, Chlamydia, and Gonorrhea are curable with the right medication. It is vital to finish the entire prescription to ensure the infection is gone.",
            options = listOf(
                QuizOption("opt-t5a", "The infection will eventually go away on its own without help.", false),
                QuizOption("opt-t5b", "You will be given a prescription for antibiotics to cure the infection.", true),
                QuizOption("opt-t5c", "You will have to stop having sex for the rest of your life.", false),
                QuizOption("opt-t5d", "There is no treatment available for bacterial STDs.", false)
            )
        ),
        QuizQuestion(
            id = "q-teststd-6",
            title = "If you are diagnosed with an STD, what is a key step in preventing further spread?",
            explanation = "Telling partners is essential. If a partner is not treated, they can pass the infection back to you or to other people, even if you have already finished your own treatment.",
            options = listOf(
                QuizOption("opt-t6a", "Simply waiting for symptoms to disappear before having sex again.", false),
                QuizOption("opt-t6b", "Informing your recent partners so they can also get tested and treated.", true),
                QuizOption("opt-t6c", "Doubling up on condoms for the next two weeks.", false),
                QuizOption("opt-t6d", "Drinking extra water to flush the infection out of your system.", false)
            )
        )
    )
)


val periodsQuiz = Quiz(
    id = "q-all-about-periods",
    articleId = "all-about-periods",
    title = "Understanding Your Period",
    questions = listOf(
        QuizQuestion(
            id = "q-period-1",
            title = "What is a period (menstruation) biologically?",
            explanation = "Each month, the uterus grows a thick lining to prepare for a possible pregnancy. If no pregnancy occurs, the body sheds that lining through the vagina, which is what we call a period.",
            options = listOf(
                QuizOption("opt-p1a", "A sign that the body is fighting an infection.", false),
                QuizOption("opt-p1b", "The monthly shedding of the lining of the uterus.", true),
                QuizOption("opt-p1c", "A permanent loss of blood that does not replenish.", false),
                QuizOption("opt-p1d", "Something that only happens when a person is sick.", false)
            )
        ),
        QuizQuestion(
            id = "q-period-2",
            title = "At what age do most people start their period?",
            explanation = "While 12 is the average age, everyone's body follows its own clock. Factors like genetics and nutrition can influence when puberty starts.",
            options = listOf(
                QuizOption("opt-p2a", "Exactly on their 10th birthday.", false),
                QuizOption("opt-p2b", "Anywhere between ages 10 and 15, with the average being 12.", true),
                QuizOption("opt-p2c", "Only once they have reached their full adult height.", false),
                QuizOption("opt-p2d", "Usually between the ages of 16 and 18.", false)
            )
        ),
        QuizQuestion(
            id = "q-period-3",
            title = "How is the 'menstrual cycle' measured?",
            explanation = "A cycle is the time from the start of one period to the start of the next. While 28 days is the 'textbook' average, cycles can range from 21 to 35 days and still be normal.",
            options = listOf(
                QuizOption("opt-p3a", "From the first day of one period to the first day of the next.", true),
                QuizOption("opt-p3b", "From the last day of one period to the last day of the next.", false),
                QuizOption("opt-p3c", "By counting exactly 30 days every single month.", false),
                QuizOption("opt-p3d", "By the number of days a person feels 'premenstrual' symptoms.", false)
            )
        ),
        QuizQuestion(
            id = "q-period-4",
            title = "What causes the 'cramps' that many people feel during their period?",
            explanation = "Prostaglandins help the uterus push out its lining. High levels of these chemicals can cause the muscles to tighten, leading to the discomfort known as cramps.",
            options = listOf(
                QuizOption("opt-p4a", "Lack of sleep and poor hydration.", false),
                QuizOption("opt-p4b", "Chemicals called prostaglandins that make the uterine muscles contract.", true),
                QuizOption("opt-p4c", "The bones in the pelvis shifting position.", false),
                QuizOption("opt-p4d", "Digestion slowing down during the week of the period.", false)
            )
        ),
        QuizQuestion(
            id = "q-period-5",
            title = "Which of the following is an internal period product (worn inside the vagina)?",
            explanation = "Tampons and menstrual cups are designed to be inserted into the vagina to catch or absorb flow before it leaves the body. Pads and liners are worn externally on underwear.",
            options = listOf(
                QuizOption("opt-p5a", "Sanitary napkins (pads).", false),
                QuizOption("opt-p5b", "Pantyliners.", false),
                QuizOption("opt-p5c", "Tampons or menstrual cups.", true),
                QuizOption("opt-p5d", "Period underwear.", false)
            )
        ),
        QuizQuestion(
            id = "q-period-6",
            title = "Why is it common for periods to be irregular for the first year or two?",
            explanation = "During puberty, it takes time for the body’s hormones to find a regular rhythm. It is very common for a person to have a period one month and then miss a month during the first couple of years.",
            options = listOf(
                QuizOption("opt-p6a", "Because the body is still adjusting to new hormone levels.", true),
                QuizOption("opt-p6b", "It means there is a serious medical problem that needs immediate surgery.", false),
                QuizOption("opt-p6c", "It only happens if the person is exercising too much.", false),
                QuizOption("opt-p6d", "Because the body is 'skipping' months to save blood.", false)
            )
        )
    )
)

val hivAidsQuiz = Quiz(
    id = "q-understanding-hiv-aids",
    articleId = "how-do-people-get-aids",
    title = "Understanding HIV & AIDS",
    questions = listOf(
        QuizQuestion(
            id = "q-hiv-1",
            title = "What is the biological difference between HIV and AIDS?",
            explanation = "HIV (Human Immunodeficiency Virus) is the actual virus. AIDS (Acquired Immune Deficiency Syndrome) is a diagnosis given when the immune system becomes seriously damaged by HIV.",
            options = listOf(
                QuizOption("opt-h1a", "They are two different types of viruses that attack the lungs.", false),
                QuizOption("opt-h1b", "HIV is the virus that attacks the immune system, and AIDS is the advanced stage of that infection.", true),
                QuizOption("opt-h1c", "AIDS is the virus you catch, and HIV is the medicine used to treat it.", false),
                QuizOption("opt-h1d", "There is no difference; they are exactly the same thing.", false)
            )
        ),
        QuizQuestion(
            id = "id-hiv-2",
            title = "How does HIV primarily affect the human body?",
            explanation = "HIV targets the immune system’s 'command center' (T cells/CD4 cells). When these are destroyed, the body can no longer effectively protect itself from common germs, viruses, and fungi.",
            options = listOf(
                QuizOption("opt-h2a", "It causes a permanent rash on the skin that never goes away.", false),
                QuizOption("opt-h2b", "It breaks down the digestive system so the body cannot absorb nutrients.", false),
                QuizOption("opt-h2c", "It attacks and destroys T cells (CD4 cells), which help the body fight off infections.", true),
                QuizOption("opt-h2d", "It primarily affects the bones, making them brittle and easy to break.", false)
            )
        ),
        QuizQuestion(
            id = "q-hiv-3",
            title = "Which of the following is a primary way HIV is transmitted?",
            explanation = "HIV is not 'easy' to catch; it requires the direct exchange of specific body fluids like blood, semen, vaginal fluids, or breast milk. It cannot survive long outside the human body.",
            options = listOf(
                QuizOption("opt-h3a", "Through casual contact like hugging, shaking hands, or sharing a toilet seat.", false),
                QuizOption("opt-h3b", "Through the air when someone with the virus coughs or sneezes.", false),
                QuizOption("opt-h3c", "Through contact with infected blood, semen, vaginal fluids, or breast milk.", true),
                QuizOption("opt-h3d", "Through insect bites, such as from mosquitoes or ticks.", false)
            )
        ),
        QuizQuestion(
            id = "q-hiv-4",
            title = "Is there currently a cure for HIV?",
            explanation = "While there is no cure, Antiretroviral Therapy (ART) stops the virus from replicating. This allows people to live long lives and can lower the virus to 'undetectable' levels, meaning it cannot be passed to others.",
            options = listOf(
                QuizOption("opt-h4a", "Yes, a one-week course of antibiotics can completely remove the virus.", false),
                QuizOption("opt-h4b", "No, but there are highly effective treatments (ART) that allow people to live long, healthy lives.", true),
                QuizOption("opt-h4c", "Yes, but the cure only works for people who catch it very early.", false),
                QuizOption("opt-h4d", "No, HIV is always a terminal illness that results in death within a year.", false)
            )
        ),
        QuizQuestion(
            id = "q-hiv-5",
            title = "Why is it important for a pregnant person with HIV to receive medical treatment?",
            explanation = "Without treatment, there is a risk of passing the virus to the baby. However, with proper medical care and medicine, the risk of a baby being born with HIV is now less than 1%.",
            options = listOf(
                QuizOption("opt-h5a", "To help them gain weight during the pregnancy.", false),
                QuizOption("opt-h5b", "To prevent the virus from being passed to the baby during pregnancy or birth.", true),
                QuizOption("opt-h5c", "Because HIV medicine is the only way to ensure the baby is the correct gender.", false),
                QuizOption("opt-h5d", "It isn't important; HIV cannot be passed from a parent to a child.", false)
            )
        ),
        QuizQuestion(
            id = "q-hiv-6",
            title = "What is the most effective way for sexually active people to prevent getting or passing HIV?",
            explanation = "Condoms provide a physical barrier that prevents fluid exchange. Appearance is not a reliable indicator of health, as many people with HIV look and feel perfectly healthy.",
            options = listOf(
                QuizOption("opt-h6a", "Washing thoroughly with soap and water immediately after sex.", false),
                QuizOption("opt-h6b", "Using a condom correctly every single time they have sex.", true),
                QuizOption("opt-h6c", "Only having sex with people who look healthy and fit.", false),
                QuizOption("opt-h6d", "Taking a multivitamin every day to boost the immune system.", false)
            )
        )
    )
)

val disclosureQuiz = Quiz(
    id = "q-disclosing-std",
    articleId = "art-disclosing-std",
    title = "Telling Your Partner",
    questions = listOf(
        QuizQuestion(
            id = "q-disclose-1",
            title = "What is the primary reason the article suggests 'reversing roles' before having the talk?",
            explanation = "Imagining yourself as the recipient helps you frame the conversation with the empathy and honesty you would want to receive.",
            options = listOf(
                QuizOption("opt-1a", "To help you come up with a way to hide the diagnosis.", false),
                QuizOption("opt-1b", "To help you understand what your partner might expect and feel.", true),
                QuizOption("opt-1c", "To figure out if your partner also has an infection.", false),
                QuizOption("opt-1d", "To practice being angry so you can defend yourself.", false)
            )
        ),
        QuizQuestion(
            id = "q-disclose-2",
            title = "If you haven’t had sex yet, how does the article suggest starting the conversation?",
            explanation = "Clear, direct language like \"Before we have sex, I want us to talk about STDs...\" ensures there are no misunderstandings.",
            options = listOf(
                QuizOption("opt-2a", "By waiting for the partner to ask about your medical history.", false),
                QuizOption("opt-2b", "By being direct and mentioning the STD and protection needs.", true),
                QuizOption("opt-2c", "By sending an anonymous email so you don't have to see their face.", false),
                QuizOption("opt-2d", "By bringing it up only if the partner refuses to use a condom.", false)
            )
        ),
        QuizQuestion(
            id = "q-disclose-3",
            title = "What should you do if your partner has a lot of questions you cannot answer?",
            explanation = "It’s okay not to know everything; seeking out professional medical facts together builds trust and ensures accuracy.",
            options = listOf(
                QuizOption("opt-3a", "Make up an answer so you don't look like you're uninformed.", false),
                QuizOption("opt-3b", "Tell them the questions aren't important.", false),
                QuizOption("opt-3c", "Offer to look for the answers together online or at a health clinic.", true),
                QuizOption("opt-3d", "Change the subject to something more positive.", false)
            )
        ),
        QuizQuestion(
            id = "q-disclose-4",
            title = "Why is it important to 'listen rather than doing all the talking' during this conversation?",
            explanation = "People need space to handle surprise or panic. Listening shows you respect their feelings and are confident in the conversation.",
            options = listOf(
                QuizOption("opt-4a", "Because you should let the partner decide if they want to break up immediately.", false),
                QuizOption("opt-4b", "Because the partner may be surprised and needs time to process and react.", true),
                QuizOption("opt-4c", "Because talking too much will make the virus spread faster.", false),
                QuizOption("opt-4d", "Because you should wait for the partner to apologize to you.", false)
            )
        ),
        QuizQuestion(
            id = "q-disclose-5",
            title = "How should you handle the partner's decision-making process after the disclosure?",
            explanation = "Giving space shows you are in control and respect their right to process personal health information before making decisions about sex or the relationship.",
            options = listOf(
                QuizOption("opt-5a", "Push them to promise they will stay with you forever.", false),
                QuizOption("opt-5b", "Give them space and suggest they take time to think about it.", true),
                QuizOption("opt-5c", "Tell them they have to decide within the next five minutes.", false),
                QuizOption("opt-5d", "Avoid talking to them for a month so they don't get overwhelmed.", false)
            )
        ),
        QuizQuestion(
            id = "q-disclose-6",
            title = "Sharing the specific type of STD you have is recommended because:",
            explanation = "Openness reduces the stigma and allows the partner to understand the specific risks and treatments associated with that infection.",
            options = listOf(
                QuizOption("opt-6a", "It is required by law to show a medical certificate.", false),
                QuizOption("opt-6b", "It helps the partner feel more comfortable and shows you are open to talking.", true),
                QuizOption("opt-6c", "It allows the partner to tell their friends exactly what you have.", false),
                QuizOption("opt-6d", "It makes the infection go away faster.", false)
            )
        )
    )
)

val allQuizzes = listOf(
    saferSexQuiz,
    testingQuiz,
    periodsQuiz,
    hivAidsQuiz,
    disclosureQuiz,
)

//fun getQuizById(id: String): Quiz? = allQuizzes.find { it.id == id }
