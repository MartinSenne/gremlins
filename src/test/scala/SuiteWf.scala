/**  ____    __    ____  ____  ____,,___     ____  __  __  ____
 *  (  _ \  /__\  (_   )(_  _)( ___)/ __)   (  _ \(  )(  )(  _ \           Read
 *   )   / /(__)\  / /_  _)(_  )__) \__ \    )___/ )(__)(  ) _ <     README.txt
 *  (_)\_)(__)(__)(____)(____)(____)(___/   (__)  (______)(____/    LICENSE.txt
 */

import junit.framework.Test;
import junit.framework.TestSuite;

import org.scalatest.junit._
import org.scalatest.SuperSuite

/** main test suite */
class SuiteWfNewXX extends SuperSuite (
  List (
    new razie.wf.WfLibTest
  )
)

class SuiteWf () extends junit.framework.TestSuite(classOf[XNadaWf]) {
  
  // this is where you list the tests...
  addTest(new junit.framework.TestSuite(classOf[razie.wf.WfLibTest]))
  addTest(new junit.framework.TestSuite(classOf[razie.wf.WfBaseTest]))
  addTest(new junit.framework.TestSuite(classOf[razie.wf.DslSimpleTest]))
  addTest(new junit.framework.TestSuite(classOf[razie.wf.OtherTest]))
  
  addTest(new junit.framework.TestSuite(classOf[razie.wf.lib.CspTest]))
  addTest(new junit.framework.TestSuite(classOf[razie.wf.lib.PiTest]))
  
  addTest(new junit.framework.TestSuite(classOf[razie.wf.lib.GTest]))
   
   def test1() = 
     // don't touch this line
     addTest(new junit.framework.TestSuite(classOf[razie.wf.WfLibTest]))
     
}

class XNadaWf extends junit.framework.TestCase {
 def testNada : Unit =  {}
}