import zio.ZIO
import zio.test.{ZIOSpecDefault, assertTrue}

object Spec extends ZIOSpecDefault {
  def returnString(str: String): ZIO[Any, Nothing, String] =
    ZIO.succeed(str)
  override def spec = suite("TestingApplicationsExamplesSpec")(
    test("returnString correctly returns string") {
      val testString = "Hello World!"
      for {
        output <- returnString(testString)
      } yield assertTrue(output == testString)
    }
  )
}
