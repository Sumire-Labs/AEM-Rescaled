import com.yogpc.qp.utils.ListLikeMap

/** Read the published Scala 3.10 TASTy, including generic methods and default arguments. */
object AemScalaApiConsumer {
  def lookup: Option[Int] =
    ListLikeMap.empty[String, Int].updated("quarry", 1).get("quarry")
}
