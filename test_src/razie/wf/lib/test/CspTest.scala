package razie.wf.lib.test

import org.scalatest.junit._
import razie.actionables._
import razie.actionables.library._

//import razie.wf._
import razie.wf._

/** CSP examples */
class CspTest extends JUnit3Suite {
  import razie.wf.lib.CSP._
  import razie.wf.lib.PiCalc.v
 
  def P = log($0 + "-P")
  def Q = log($0 + "-Q")
  def T = log($0) // transparent

  def c = Channel("c") 
  
  def myp41 = v(c) (c ? P | c ! Q)  // correct v(c) ( c(0) P | c<0> Q )
  def testmyp41 = expect (true) { ((myp41.print run "1").asInstanceOf[List[_]] contains "1-Q-P") }
  
  override def setUp () = { Engines.start }
  override def tearDown () = { Engines().stop }
}
