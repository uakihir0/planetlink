package work.socialhub.planetlink

import work.socialhub.planetlink.misskey.model.MisskeyUser
import work.socialhub.planetlink.model.Comment
import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.User

object PrintClass {

    fun AbstractTest.dump(
        user: User,
        sp: String = "",
    ) {
        println("${sp}>> USER")
        println("${sp}ID    > ${user.id!!.value}")
        println("${sp}Name  > ${user.name}")
        println("${sp}Url   > ${user.webUrl}")
        println("${sp}Icon  > ${user.iconImageUrl}")

//        if (user is MastodonUser) {
//            for (filed in mastodonUser.getFields()) {
//                println(filed.getName() + ":" + filed.getValue())
//            }
//        }
//
        if (user is MisskeyUser) {
            for (filed in user.fields) {
                println("${sp}Field > ${filed.name} : ${filed.value?.displayText}")
            }
            user.avatarColor?.let {
                println("${sp}RGB   > ${it.toJavaScriptFormat()}")
            }
        }
//
//        if (user is TumblrUser) {
//            System.out.println((user as TumblrUser).getBlogUrl())
//        }
    }

    fun AbstractTest.dump(
        comment: Comment,
        sp: String = ""
    ) {
        val shared = comment.sharedComment

        if (shared != null) {
            println("${sp}>> SHARED COMMENT")
            dump(shared, "$sp| ")

        } else {
            println("${sp}>> COMMENT")
            println("${sp}Text > ${comment.text?.displayText}")
            println("${sp}Url  > ${comment.webUrl}")
            comment.user?.let { dump(it, "$sp| ") }
        }
    }

    fun AbstractTest.dumpComments(
        comments: Pageable<Comment>,
        sp: String = "",
    ) {
        comments.entities.forEach {
            dump(it, sp)
            println()
        }
    }

    fun AbstractTest.dumpUsers(
        users: Pageable<User>,
        sp: String = "",
    ) {
        users.entities.forEach {
            dump(it, sp)
            println()
        }
    }
}