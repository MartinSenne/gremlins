package razie.wf.test

import org.scalatest.junit._
import razie.actionables._
import razie.actionables.library._

//import razie.wf._
import razie.wf._

class WfBaseTest extends JUnit3Suite {
  import wf._

  //-------------------------------- if
 
  // TODO problem: welse needs to be on the same line as the closing  } of its own wif
  
  def wif1 = wif (_ == 1) {  // no good, really - condition not serializable
     wf.inc + wf.log ($0) 
   } welse { 
     wf.inc + wf.inc + wf.log ($0) 
   }
   
  def wif2 = wif ("$0 == 1") {  // good, condition serializable
     wf.inc + wf.log ($0) 
   } welse { 
     wf.inc + wf.log ($0) 
   }
   
  def wif2s = """if ($0 == 1) then {
     inc; log ($0) 
   } else { 
     inc; log ($0) 
   }"""

  def wif3s = """if ($0 == 1) then inc else log ($0)"""

  case class MS (s:String) { def print = { println(s); s} }
  implicit def p (s:String) : MS = MS(s)
  
  def testwif11 = expect (2) { wif1.print run 1 }
  def testwif12 = expect (4) { wif1.print run 2 }
  def testwif21 = expect (2) { wif2.print run 1 }
  def testwif22 = expect (3) { wif2.print run 2 } // TODO should be 4...currently not implemented properly :)
  def testwif23 = expect (3) { wf(wif2s) run 2 }
  def testwif23s = expect (2) { wf(wif3s) run 2 }
  def testwif24 = expect (3) { wf((wf toDsl wif2).print).print run 2 }

  // ---------------- seq
  
  def wseq1 = wf.seq(wf.inc, wf.log($0))
  def wseq2 = wf.nop + inc + set(5) + inc + wf.log($0)
  def w2s = """seq { nop; inc; log($0) }"""
  def w3s = """seq { seq { nop; inc; log($0) }; seq { nop; inc; log($0) } }"""
  def w1m = """
nop
inc
log($0)
"""
  
  def testwseq1 = expect (2) { wseq1.print run 1 }
  def testwseq2 = expect (6) { wseq2.print run 1 }
  def testwseq1s = expect (2) { wf(wf toDsl wseq1) run 1 }
  def testw2s = expect (2) { wf(w2s) run 1 }
  def testw3s = expect (3) { wf(w3s) run 1 }
  def testw1m = expect (2) { wf(w1m) run 1 }

  // ---------------- par
    
  def wpar1 = wf.par(log(1), wf.log(2))

  def testwpar1  = expect (1::2::Nil) { razie.M.anyOrder (wpar1.print run 1) }

  def wpar2 = nop | (nop + inc + log($0)) | (inc + log($0))
  def wpar3 = inc + log($0) | inc + log($0)
  def wpar4 = """par { seq { inc; log($0) }; seq { inc; log($0) } }"""
  def wpar5 = """
par { 
  seq {
  	 inc
  	 log($0)
  	 } 
  seq { 
    inc
    log($0)
    }
  }"""

  def testwpar2 = expect (1::2::2::Nil) { razie.M.anyOrder (wpar2.print run 1) }
  def testwpar3 = expect (2::2::Nil) { (wpar3 run 1) }
  def testwpar2s = expect (1::2::2::Nil) { razie.M.anyOrder (wf(wf toDsl wpar2.print) run 1) }
  def testwpar4 = expect (2::2::Nil) { (wf(wpar4) run 1) }
  def testwpar5 = expect (2::2::Nil) { (wf(wpar5).print run 1) }

  //-------------------------- match/case
  
  def wm1 = wmatch2 ($0) {
    wcase2[Int] (1) {log ("m 1")} +
    wcase2[Int] (2) {log ("m 2")} +
    wcase2      ("Gigi") {log ("m Gigi")} +
    wcase2      (List(1,2,3)) {log ("m 1,2,3")} +
    wcaseany2   {log ("matched none")}
  }
    
  def testw1 = expect ("m 1") { wm1.print run 1 }
  def testw2 = expect ("m 2") { wm1 run 2 }
  def testw3 = expect ("m Gigi") { wm1 run "Gigi" }
  def testw4 = expect ("m 1,2,3") { wm1.print run List(1,2,3) }

}
