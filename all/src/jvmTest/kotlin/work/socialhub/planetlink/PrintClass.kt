package work.socialhub.planetlink

import work.socialhub.planetlink.model.User

object PrintClass {

    fun AbstractTest.dump(user: User) {
        println("Name > ${user.name}")
        println("Url  > ${user.webUrl}")
        println("Icon > ${user.iconImageUrl}")

//        if (user is MastodonUser) {
//            for (filed in mastodonUser.getFields()) {
//                println(filed.getName() + ":" + filed.getValue())
//            }
//        }
//
//        if (user is MisskeyUser) {
//            for (filed in misskeyUser.getFields()) {
//                println(filed.getName() + ":" + filed.getValue().getDisplayText())
//            }
//            if (misskeyUser.getAvatarColor() != null) {
//                System.out.println("RGB:" + misskeyUser.getAvatarColor().toJavaScriptFormat())
//            }
//        }
//
//        if (user is TumblrUser) {
//            System.out.println((user as TumblrUser).getBlogUrl())
//        }
    }
}