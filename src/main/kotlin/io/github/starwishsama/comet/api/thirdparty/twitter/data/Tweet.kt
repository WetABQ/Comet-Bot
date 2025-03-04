/*
 * Copyright (c) 2019-2021 StarWishsama.
 *
 * 此源代码的使用受 GNU General Affero Public License v3.0 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 *  Use of this source code is governed by the GNU AGPLv3 license which can be found through the following link.
 *
 * https://github.com/StarWishsama/Comet-Bot/blob/master/LICENSE
 *
 */

package io.github.starwishsama.comet.api.thirdparty.twitter.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.github.starwishsama.comet.CometVariables
import io.github.starwishsama.comet.CometVariables.hmPattern
import io.github.starwishsama.comet.CometVariables.mapper
import io.github.starwishsama.comet.api.thirdparty.twitter.TwitterApi
import io.github.starwishsama.comet.api.thirdparty.twitter.data.tweetEntity.Media
import io.github.starwishsama.comet.objects.wrapper.MessageWrapper
import io.github.starwishsama.comet.objects.wrapper.buildMessageWrapper
import io.github.starwishsama.comet.utils.NumberUtil.getBetterNumber
import io.github.starwishsama.comet.utils.StringUtil.limitStringSize
import io.github.starwishsama.comet.utils.StringUtil.toFriendly
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.MessageChain
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.regex.Pattern
import kotlin.time.toKotlinDuration

val tcoPattern: Pattern = Pattern.compile("https://t.co/[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]")

data class Tweet(
    @JsonProperty("created_at")
    val postTime: String,
    val id: Long,
    @JsonProperty("id_str")
    val idAsString: String,
    @JsonProperty("full_text")
    val text: String,
    val truncated: Boolean,
    val entities: JsonNode?,
    val source: String,
    @JsonProperty("in_reply_to_status_id")
    val replyTweetId: Long?,
    val user: TwitterUser,
    @JsonProperty("retweeted_status")
    val retweetStatus: Tweet?,
    @JsonProperty("retweet_count")
    val retweetCount: Long?,
    @JsonProperty("favorite_count")
    val likeCount: Long?,
    @JsonProperty("possibly_sensitive")
    val sensitive: Boolean?,
    @JsonProperty("quoted_status")
    val quotedStatus: Tweet?,
    @JsonProperty("is_quote_status")
    val isQuoted: Boolean
) {
    /**
     * 格式化输出推文
     */
    fun toMessageWrapper(): MessageWrapper {
        val duration =
            Duration.between(getSentTime(), LocalDateTime.now())
        val extraText =
            "❤${likeCount?.getBetterNumber()} | \uD83D\uDD01${retweetCount} | 🕘${hmPattern.format(getSentTime())} - ${
                duration.toKotlinDuration().toFriendly(msMode = false)
            } 前"

        if (retweetStatus != null) {
            return buildMessageWrapper {
                addText("♻ 转推自 ${retweetStatus.user.name}:\n")
                addText("${retweetStatus.text.cleanShortUrl().limitStringSize(150)}\n")
                addPictureByURL(getPictureUrl())
                addText("$extraText\n")
                addText("\uD83D\uDD17 > ${getTweetURL()}\n")
            }
        }

        if (isQuoted && quotedStatus != null) {
            return buildMessageWrapper {
                addText("♻ ${user.name} 转推并评论说\n")
                addText(text.cleanShortUrl() + "\n\n")
                addText("💬 ${quotedStatus.user.name} >\n")
                addText(quotedStatus.text.cleanShortUrl().limitStringSize(50) + "\n")
                addPictureByURL(getPictureUrl())
                addText("$extraText\n🔗 > ${getTweetURL()}\n")
            }
        }

        if (replyTweetId != null) {
            val repliedTweet = TwitterApi.getTweetById(replyTweetId)

            return buildMessageWrapper {
                addText("\uD83D\uDCAC ${user.name} 回复推文\n")
                addText(text.cleanShortUrl() + "\n\n")
                addText("\uD83D\uDCAC ${repliedTweet?.user?.name}\n")
                addText("${repliedTweet?.text?.cleanShortUrl()?.limitStringSize(50)}")
                addPictureByURL(getPictureUrl())
                addText("$extraText\n🔗 > ${getTweetURL()}\n")
            }
        }

        return buildMessageWrapper {
            addText("${text.cleanShortUrl()}\n")
            addText("$extraText\n")
            addPictureByURL(getPictureUrl())
            addText("🔗 > ${getTweetURL()}\n")
        }
    }

    /**
     * 判断两个推文是否内容相同
     */
    fun contentEquals(tweet: Tweet?): Boolean {
        if (tweet == null) return false
        return id == tweet.id || text == tweet.text || getSentTime().isEqual(tweet.getSentTime())
    }

    /**
     * 获取该推文发送的时间
     */
    fun getSentTime(): LocalDateTime {
        val twitterTimeFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH)
        return twitterTimeFormat.parse(postTime).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    /**
     * 获取推文中的第一张图片
     */
    private fun getPictureUrl(nestedMode: Boolean = false): String? {

        /**
         * 从此推文中获取图片链接
         */

        val media = entities?.get("media")
        if (media != null) {
            try {
                val image = mapper.readValue(media[0].traverse(), Media::class.java)
                if (image.isSendableMedia()) {
                    return image.getImageUrl()
                }
            } catch (e: RuntimeException) {
                CometVariables.logger.warning("在获取推文下的图片链接时发生了问题", e)
            }
        }

        // 避免套娃
        if (!nestedMode) {
            /**
             * 如果推文中没有图片, 则尝试获取转推中的图片
             */
            if (retweetStatus != null) {
                return retweetStatus.getPictureUrl(true)
            }

            /**
             * 如果推文中没有图片, 则尝试获取引用回复推文中的图片
             */
            if (quotedStatus != null) {
                return quotedStatus.getPictureUrl(true)
            }
        }

        return null
    }

    /**
     * 清理推文中末尾的 t.co 短链
     */
    private fun String.cleanShortUrl(): String {
        val tcoUrl = mutableListOf<String>()

        tcoPattern.matcher(this).run {
            while (find()) {
                tcoUrl.add(group())
            }
        }

        return if (tcoUrl.isNotEmpty()) this.replace(tcoUrl.last(), "") else this
    }

    fun toMessageChain(target: Contact): MessageChain {
        return runBlocking { toMessageWrapper().toMessageChain(target) }
    }

    private fun getTweetURL(): String {
        return "https://twitter.com/${user.twitterId}/status/$idAsString"
    }
}
