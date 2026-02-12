package net.tuurlievens.fuzzyclock.text

class FuzzyTextDutch : FuzzyTextInterface {

    private fun hourText(h: Int): String = when ((h % 12 + 12) % 12) {
        1 -> "één"
        2 -> "twee"
        3 -> "drie"
        4 -> "vier"
        5 -> "vijf"
        6 -> "zes"
        7 -> "zeven"
        8 -> "acht"
        9 -> "negen"
        10 -> "tien"
        11 -> "elf"
        0 -> "twaalf"
        else -> "twee"
    }

    private fun minuteText(m: Int): String = when (m) {
        1 -> "één"
        2 -> "twee"
        3 -> "drie"
        4 -> "vier"
        5 -> "vijf"
        6 -> "zes"
        7 -> "zeven"
        8 -> "acht"
        9 -> "negen"
        10 -> "tien"
        11 -> "elf"
        12 -> "twaalf"
        13 -> "dertien"
        14 -> "veertien"
        15 -> "vijftien"
        else -> "$m"  // fallback
    }

    override fun generate(hour: Int, min: Int): String {
        val h24 = ((hour % 24) + 24) % 24
        val hr = if (h24 == 0) 12 else h24
        val nextHr = if (h24 == 23) 0 else h24 + 1

        val hrText = hourText(hr)
        val hrTextNext = hourText(nextHr)

        if (min == 0) {
            return when (h24) {
                0 -> "het is middernacht"
                12 -> "het is twaalf uur"
                else -> "het is $hrText uur"
            }
        }

        val mText = minuteText(min)

        return when {
            // 1–14 over: "één over ... tot twaalf over"
            min in 1..14 -> "het is $mText over $hrText"

            // 15: kwart over
            min == 15 -> "het is kwart over $hrText"

            // 16–29: "X voor half <next>"
            min in 16..29 -> {
                val mBeforeHalf = 30 - min
                val mTextBefore = minuteText(mBeforeHalf)
                "het is $mTextBefore voor half $hrTextNext"
            }

            // 30: half
            min == 30 -> "het is half $hrTextNext"

            // 31–44: "X over half <next>"
            min in 31..44 -> {
                val mAfterHalf = min - 30
                val mTextAfter = minuteText(mAfterHalf)
                "het is $mTextAfter over half $hrTextNext"
            }

            // 45: kwart voor
            min == 45 -> "het is kwart voor $hrTextNext"

            // 46–59: "X voor <next>"
            min in 46..59 -> {
                val mBeforeHour = 60 - min
                val mTextBefore = minuteText(mBeforeHour)
                "het is $mTextBefore voor $hrTextNext"
            }

            else -> "het is $hrText uur"
        }
    }
}
