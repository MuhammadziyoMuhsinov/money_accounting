package drawable

data class FAQItem(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)
