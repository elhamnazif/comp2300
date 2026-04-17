package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.education.Article
import com.group8.comp2300.domain.model.education.ArticleImage
import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.mock.ContentCategory

val article1 = Article(
    id = "safer-sex-basics",
    title = "Safer Sex",
    description = "How to reduce the risk of STIs through barrier methods, regular testing, and choosing lower-risk sexual activities.",
    content = """
        Safer sex is all about protecting yourself and your partners from sexually transmitted infections (STIs). It helps you stay healthy and can even make sex better by reducing worry.

        ### How Barriers Protect You
        One of the best ways to make sex safer is by using a barrier every single time you have oral, anal, or vaginal sex. 
        * **Condoms & Internal Condoms:** Protect against sexual fluids and some skin-to-skin contact.
        * **Dental Dams:** Used for oral sex to prevent fluid exchange.
        * **Gloves:** Can be used during manual stimulation to avoid passing fluids via small cuts on hands.

        ### The Role of Testing
        Getting tested regularly is part of safer sex, even if you always use barriers and feel fine. Most people with STIs don’t have symptoms. Testing is the only way to know for sure and get the right treatment.

        ### Understanding Risks by Activity
        Different activities carry different levels of risk for spreading infections like Chlamydia, Gonorrhea, Syphilis, HIV, and HPV.
        * **Risk-Free:** Masturbation.
        * **Lower Risk:** Kissing, using clean and sterilized sex toys.
        * **High Risk:** Vaginal or anal sex without a condom.

        ### Tips for Success
        * **Wash hands and toys:** Clean sex toys with soap and water before they touch another person.
        * **Avoid excessive substances:** Alcohol and drugs can make it harder to make safe decisions.
        * **Communication:** Always tell sexual partners if you have an STI before having sex so you can make a plan together.
    """.trimIndent(),
    thumbnailUrl = "https://flo.health/cdn-cgi/image/quality=85,format=auto/uploads/media/sulu-1000x-inset/05/10085-different%20types%20of%20birth%20control%20to%20prevent%20an%20sti.jpg?v=1-0",
    publisher = "Planned Parenthood",
    publishedDate = 1712920000000L, // Approximated for 2024
    categories = listOf(
        ContentCategory.STI_PREVENTION,
        ContentCategory.SEXUAL_HEALTH,
        ContentCategory.CONTRACEPTION
    ),
    images = listOf(
        ArticleImage("img1","https://nextcare.com/wp-content/uploads/2024/11/infographic-how-to-practice-safe-sex-scaled.jpg", "Infographic on hot to prevent sexually transmitted disease, published by Next Care.")
    )
)

val article2 = Article(
    id = "getting-tested-for-stds",
    title = "Getting Tested for STDs",
    description = "Practical steps for getting a sexual health checkup, including how tests are performed, symptom identification, and the importance of testing even when feeling fine.",
    content = """
        Most of the time, STDs have no symptoms. Testing is the only way to know for sure if you have an infection. If you’ve had any kind of sexual contact—including vaginal, anal, or oral sex—you should talk with a healthcare provider about getting tested.

        ### I think I have symptoms. Should I get tested?
        Yes. If you notice any signs, see a doctor immediately. Symptoms can come and go, but that doesn't mean the infection is gone. Common signs include:
        * Sores or bumps on or around your genitals, thighs, or butt cheeks.
        * Unusual discharge from the vagina or penis.
        * Burning when you pee or feeling the need to pee frequently.
        * Itching, pain, irritation, or swelling in the genital or anal area.
        * Flu-like symptoms such as fever, body aches, and swollen glands.

        Many of these can be caused by non-STD issues like UTIs or yeast infections, so a professional test is the only way to be sure. Be honest with your provider about your symptoms, the types of sexual contact you've had, and your use of protection.

        ### Why testing matters
        Some STDs can cause serious health problems if left untreated. Having one STD can also make you more likely to contract others, like HIV. Finding out early allows you to start treatment faster and avoid passing the infection to others.

        ### I feel fine—do I still need a test?
        You cannot tell if you have an STD just by looking or feeling. Because most people don't show symptoms, you could be carrying and spreading an infection without knowing it. It is especially important to get tested if you have had unprotected sexual contact or if a partner reveals they have an STD.

        ### What to expect
        The idea of testing can be scary, but remember: most common STDs are easily cured with medicine. For those that aren't curable, treatments exist to manage symptoms and lower the risk of transmission. Testing is a quick, often painless, and responsible part of taking care of yourself.
    """.trimIndent(),
    thumbnailUrl = "https://images.unsplash.com/vector-1775025870126-687b2cbfabff?q=80&w=1026&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
    publisher = "Northwester Medicine",
    publishedDate = 1713100000000L, // April 2024
    categories = listOf(
        ContentCategory.STI_PREVENTION,
        ContentCategory.SEXUAL_HEALTH
    ),
    images = listOf(
        ArticleImage("img2", "https://www.utsc.utoronto.ca/hwc/sites/utsc.utoronto.ca.hwc/files/styles/original_image/public/images/page/STI%20test.PNG?itok=B97gnaC1", "Types of tests for STDs"),
        ArticleImage("img3","https://www.nm.org/-/media/northwestern/healthbeat/images/healthy-tips/nm-sti-testing_infographic.jpg", "An infographic published by Northwestern Medicine: STI Testing - Who, What and When?")
    )
)

val article3 = Article(
    id = "all-about-periods",
    title = "All About Periods",
    description = "A comprehensive guide to the biological process of the menstrual cycle, managing symptoms, and understanding reproductive health.",
    content = """
        A period is the monthly shedding of the uterine lining (endometrium). It is a normal, healthy sign that the reproductive system is functioning correctly. While it is a milestone of puberty, it also signifies the beginning of a person's fertile years.

        ### The Menstrual Cycle Phases
        The cycle is more than just the days of bleeding; it is a complex loop controlled by hormones:
        1. **The Menstrual Phase:** The first day of your period is Day 1. This is when the uterus sheds its lining because a pregnancy did not occur.
        2. **The Follicular Phase:** Overlapping with the period, the brain sends signals to the ovaries to prepare an egg for release.
        3. **The Ovulatory Phase:** Usually around the middle of the cycle, an egg is released (ovulation). This is the "fertile window" where pregnancy is most likely.
        4. **The Luteal Phase:** The body prepares the uterine lining again. If the egg isn't fertilized, hormone levels drop, triggering the next period.

        ### Do Periods Happen Regularly When Menstruation Starts?
        For the first few years after a girl starts her period, it might not come regularly. This is normal at first. By about 2–3 years after her first period, a girl's periods should be coming around once every 4–5 weeks.

        ### Understanding Ovulation & Pregnancy Risk
        It is a common myth that you cannot get pregnant during your first period or during menstruation. 
        * **First-time risk:** Because ovulation happens *before* your first period, you can become pregnant before you ever see blood.
        * **Cycle Variance:** Sperm can live inside the body for up to 5 days. If you have a short cycle and have sex near the end of your period, you could conceive shortly after.

        ### Choosing the Right Products
        Every body is different, and your choice might change depending on your activity level:
        * **Pads:** Worn outside the body. Available in "Light," "Regular," and "Overnight" thicknesses. Great for those just starting or those who find internal products uncomfortable.
        * **Tampons:** Inserted into the vaginal canal. They provide freedom for swimming and sports. **Safety Tip:** Always use the lowest absorbency needed and change every 4–8 hours to prevent Toxic Shock Syndrome (TSS).
        * **Menstrual Cups & Discs:** Eco-friendly, reusable silicone devices that collect blood. They can often be worn for up to 12 hours.
        * **Period Underwear:** Specially designed fabric that absorbs moisture and prevents leaks, used alone or as backup for tampons/cups.

        ### Managing PMS and Physical Discomfort
        **Premenstrual Syndrome (PMS)** refers to the week before your period when you might feel bloated, break out in acne, or feel more emotional. 
        **Cramps (Dysmenorrhea):** These are caused by the uterus contracting to shed its lining. 
        * **Heat Therapy:** A heating bag or hot water bottle on the lower abdomen can relax the muscles.
        * **Nutrition:** Reducing salt and caffeine can help with bloating and irritability.
        * **Exercise:** Gentle movement like walking or yoga can actually reduce the severity of cramps.

        ### When to Consult a Professional
        Tracking your cycle with an app or calendar is the best way to know what is "normal" for you. You should call a doctor if:
        * You are 15 and have not started menstruating.
        * Your period was regular but has suddenly stopped for more than 3 months.
        * You experience "spotting" or bleeding between your expected periods.
        * You have to change your pad or tampon every hour because it is completely soaked.
        * Your pain is so severe that it prevents you from going to school or work.
    """.trimIndent(),
    thumbnailUrl = "https://images.unsplash.com/vector-1749543354893-1bd4ff661bd0?q=80&w=1074&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
    publisher = "KidsHealth (from Nemours)",
    publishedDate = 1720137600000L, // July 2024
    categories = listOf(
        ContentCategory.MENSTRUAL_HEALTH,
        ContentCategory.SEXUAL_HEALTH,
        ContentCategory.PUBERTY
    ),
    images = listOf(
        ArticleImage("img4", "https://images.ctfassets.net/aqfuj2z95p5p/1A84Dh9CVXtOOsq2nZtxhF/75b68b1df5428e345eb3274f6efc0497/Always_EN_UK_Articles_A115-All-about-the-female-menstrual-cycle_ALWAYS-Menstrual-cycle-infographic.png", "An infographic on the menstrual cycle")
    )
)

val article4 = Article(
    id = "how-do-people-get-aids",
    title = "How Do People Get AIDS?",
    description = "An explanation of the difference between HIV and AIDS, how the virus is transmitted, and the medical advancements like PrEP that help people stay healthy.",
    content = """
        Understanding the difference between HIV and AIDS is the first step toward prevention and support. While the terms are often used together, they represent different stages of a medical condition.

        ### What Are HIV and AIDS?
        * **HIV (Human Immunodeficiency Virus):** This is a virus that attacks the body's immune system, specifically the CD4 cells (T cells), which help the immune system fight off infections.
        * **AIDS (Acquired Immune Deficiency Syndrome):** This is the late stage of HIV infection that occurs when the body’s immune system is badly damaged because of the virus. 

        **Crucial Fact:** Not everyone with HIV will develop AIDS. With modern medical treatment, most people with HIV can live long, healthy lives without ever progressing to AIDS.

        ### How Does HIV Spread?
        HIV is transmitted through specific body fluids: blood, semen (cum), pre-seminal fluid, rectal fluids, vaginal fluids, and breast milk. Transmission occurs when these fluids get into the bloodstream of an HIV-negative person through a mucous membrane (found in the rectum, vagina, penis, or mouth), open cuts or sores, or by direct injection.
        
        **Common routes include:**
        * **Sexual Contact:** Most commonly through anal or vaginal sex without a condom or PrEP.
        * **Shared Needles:** Sharing equipment for injecting drugs or tattooing.
        * **Birth:** Passing from mother to child during pregnancy, childbirth, or breastfeeding (though medical intervention can now make this risk very low).

        ### Common Myths: How HIV Does NOT Spread
        It is important to know that HIV **cannot** be passed through casual contact. You cannot get HIV from:
        * Saliva, tears, or sweat.
        * Hugging, shaking hands, or closed-mouth kissing.
        * Sharing toilets, drinking glasses, or dishes.
        * Mosquitoes or other insects.

        ### Protecting Yourself: PrEP and Prevention
        * **PrEP (Pre-Exposure Prophylaxis):** This is a daily pill or injection for people who are at risk for HIV. When taken as prescribed, it is highly effective at preventing HIV from sexual contact or injection drug use.
        * **Condoms & Barriers:** Using external/internal condoms or dental dams every time reduces the risk of HIV and protects against other STIs that PrEP does not cover.
        * **Regular Testing:** Because symptoms can be mild or non-existent at first, the only way to know your status is to get tested.
        * **Treatment as Prevention (TasP):** People with HIV who take medicine (Antiretroviral Therapy or ART) and stay virally undetectable have effectively no risk of transmitting HIV to their HIV-negative partners through sex.

        ### Living with HIV
        Modern medicine has turned HIV into a manageable chronic condition. Early diagnosis is key. If you think you have been exposed, talk to a doctor immediately about **PEP (Post-Exposure Prophylaxis)**, which can prevent infection if started within 72 hours of exposure.
    """.trimIndent(),
    thumbnailUrl = "https://images.unsplash.com/vector-1753253616518-6f68deb6c3af?q=80&w=880&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
    publisher = "Everyday Health",
    publishedDate = 1717200000000L, // June 2024
    categories = listOf(
        ContentCategory.SEXUAL_HEALTH,
        ContentCategory.STI_PREVENTION
    ),
    images = listOf(
        ArticleImage("img5", "https://hivinfo.nih.gov/sites/default/files/styles/700w/public/infographics/PNG/hiv-and-aids_en.png?itok=EU74JZxg", "Infographic on the difference between HIV and AIDS, published by the National Library of Medicine (NIH)"),
        ArticleImage("img6", "https://images.everydayhealth.com/images/seo-graphic-content-initiative/how-hiv-infection-affects-the-body.png?w=1110", "Created by Everyday Health")
    )
)

val article5 = Article(
    id = "telling-your-partner-you-have-an-std",
    title = "Conversation Tips: Telling Your Partner You Have an STD",
    description = "Practical steps and communication strategies for disclosing an STD diagnosis with honesty and confidence.",
    content = """
        If you have been diagnosed with an STD, and don't know the right way to break the news to your current or past partner, this is the article for you. Make sure to inform them right away, as it is the right thing to do.

        ### Why Do I Need to Tell My Partners?
        An STD that isn’t treated can spread and cause serious health problems for you and your sex partners. 
        * Letting a past or current partner know gives that person the chance to see a doctor and get treated.
        * Telling partners you haven’t had sex with yet shows them that you care about them and their health.

        ### How Do I Tell a Partner About an STD?
        It’s normal to be nervous, but it’s best for your partner to hear it from you.
        * **Don't avoid the conversation:** "Talk with your partner(s) BEFORE having sex so you can both make informed choices about your sexual health."
        * **Be open and honest:** Be clear with your partner(s) about the number of sexual partners you have.
        * **Be understanding:** Being respectful and nonjudgmental can create the space for a more productive conversation. This also helps lay the groundwork to keep those conversations going.
        * **Let them know:** Tell your partner(s) if you have an STI, even if you're currently taking medicine to treat those infections.
        * **Ask when they were last tested:** Find out when they were last tested for STIs. Consider getting tested together.
        * **Give them space:** Don't push for decisions right away. Suggest: "I know you probably want some time to think about this."

        ### How Should I Approach my Healthcare Provider?
        * **Ask to get tested for STIs:** The sooner you are treated for an STI, the less chance an infection will impact other parts of your health.
        * **Be open and honest:** Knowing your sexual history and any symptoms you have will help your provider to provide the best possible treatment.
        * **Don't be afraid to ask questions:** You're in charge of your sexual health and deserve non-judgmental, stigma-free answers to your questions.
    """.trimIndent(),
    thumbnailUrl = "https://pbs.twimg.com/media/DYGZGZXU8AE3Rth.jpg",
    publisher = "CDC: Centres for Disease Control and Prevention",
    publishedDate = 1704067200000L, // January 2024
    categories = listOf(
        ContentCategory.CONSENT,
        ContentCategory.RELATIONSHIPS,
        ContentCategory.SEXUAL_HEALTH
    ),
    images = emptyList()
)

val allArticles = listOf(
    article1,
    article2,
    article3,
    article4,
    article5,
)




