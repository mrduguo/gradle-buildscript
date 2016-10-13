package hello

object Application {
  def main(args: Array[String]):Unit={
    sayHello("Wolrd")
  }

  def sayHello(name: String): String = {
    var msg = "Hello %s".format(name)
    println(msg)
    msg
  }
}