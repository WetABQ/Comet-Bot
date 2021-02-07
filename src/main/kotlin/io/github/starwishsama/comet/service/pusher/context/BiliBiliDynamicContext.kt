package io.github.starwishsama.comet.service.pusher.context

import com.google.gson.annotations.SerializedName
import io.github.starwishsama.comet.api.thirdparty.bilibili.data.dynamic.Dynamic
import io.github.starwishsama.comet.api.thirdparty.bilibili.data.dynamic.convertToWrapper
import io.github.starwishsama.comet.objects.push.BiliBiliUser
import io.github.starwishsama.comet.objects.wrapper.MessageWrapper

class BiliBiliDynamicContext(
    pushTarget: MutableList<Long>,
    retrieveTime: Long,
    @SerializedName("custom_status")
    override var status: PushStatus = PushStatus.READY,
    val pushUser: BiliBiliUser,
    var dynamic: Dynamic
) : PushContext(pushTarget, retrieveTime, status), Pushable {

    override fun toMessageWrapper(): MessageWrapper {
        val before = dynamic.convertToWrapper()
        return MessageWrapper("${pushUser.userName}\n" + before.text, success = before.success).also {
            before.pictureUrl.forEach { url ->
                it.plusImageUrl(url)
            }
        }
    }

    override fun contentEquals(other: PushContext): Boolean {
        if (other !is BiliBiliDynamicContext) return false

        val current = dynamic.data.card?.description?.dynamicId
        val toCompare = other.dynamic.data.card?.description?.dynamicId

        return current == toCompare && dynamic.data.card?.description?.timeStamp == other.dynamic.data.card?.description?.timeStamp
    }
}

fun Collection<PushContext>.getDynamicContext(uid: Long): BiliBiliDynamicContext? {
    val result = this.parallelStream().filter { it is BiliBiliDynamicContext && uid == it.pushUser.id.toLong() }.findFirst()
    return if (result.isPresent) result.get() as BiliBiliDynamicContext else null
}