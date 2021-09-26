package org.yusupov

import org.yusupov.server.Server

object Main extends zio.App {
  override def run(args: List[String]) =
    Server.start()
}
