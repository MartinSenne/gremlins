package razie.wf.study5

import razie.base.ActionContext

/** basic executable/actionable */
trait WFunc[T] { // extends PartialFunc ?
  def exec (in:ActionContext, prevValue:Any) : T
}

/** this is the actual work carried out by the respective action */
trait WfExec extends WFunc[Any] {
  override  def exec  (in:ActionContext, prevValue:Any) : Any = exec (prevValue)
  protected def exec  (prevValue:Any)                   : Any = { execu(); prevValue} /* default does nothing */
  protected def execu ()                                : Unit = {} /* default does nothing */
            def wname : String = "?"
}

abstract class WfLib[T] { me =>
//  val me = this
 
  def nop = me wrap new WfExec { override def execu() = {}; override def wname = "nop" }
  def log (m: => String) = me wrap new WfLog ((x,y)=>m)
  def log (m: (ActionContext, Any)=> String) = me wrap new WfLog (m)
  def assign (name:String) (value: =>Any) = me wrap new WfAssign (name)((x)=>value)
 
  // wraps an WfExec into a WfAct...customize this per implementation
  def wrap (e:WfExec) : T
}

class WfAssign (name:String) (v: Any => Any) extends WfExec {
  override def exec (in:ActionContext, prevValue:Any) = { 
    in.set (name, v(prevValue))
    prevValue
  }
  
  override def wname = "assign"
}

class WfLog (msg : (ActionContext, Any) => String) extends WfExec { 
  override def exec  (in:ActionContext, prevValue:Any) : Any = println (msg(in, prevValue))
  override def wname = "log"
}
