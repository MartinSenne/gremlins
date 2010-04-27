/**  ____    __    ____  ____  ____,,___     ____  __  __  ____
 *  (  _ \  /__\  (_   )(_  _)( ___)/ __)   (  _ \(  )(  )(  _ \           Read
 *   )   / /(__)\  / /_  _)(_  )__) \__ \    )___/ )(__)(  ) _ <     README.txt
 *  (_)\_)(__)(__)(____)(____)(____)(___/   (__)  (______)(____/    LICENSE.txt
 */
package razie.wf

import razie.AA
import razie.base.{ActionContext => AC}

//-------------------- graph processing engine

object Audit {
  private def audit (aa:AA) = razie.Audit (aa)

  def recExec  (a:WfAct, in:Any, out:Any, paths:Int) {// new session
     audit (razie.AA("activity", a.toString, "in", in.toString, "out",out.toString, "paths", paths.toString))
  }
}

/** 
 * a process thread encapsulates the state of a running workflow branch . You can see this as an 
 * actor as well. Depending on the engine, each PT may have its own processor, thread or whatever...
 * conceptually these are independent paths of execution, for the PAR/JOIN branches for isntance
 */
class ProcessThread (val start:WfAct, val startLink:Option[WL], val ctx:AC, val startValue:Any) { 
  // should i keep state inside the activities or outside in a parallel graph, built as it's traversed?
     
  var currV:Any = startValue
     
  // the activity in progress
  var currAct : WfAct = null
    
  // the activities to start on the next step
  var nextAct   = start 
  var nextLink  = startLink
   
  // pre-processing
  protected def pre = {
    currAct = nextAct
    currAct.procState = ProcState.INPROGRESS 
    nextLink map (_.linkState = LinkState.DONE)
  }
    
  // post-processing
  protected def post = {
    currAct.procState = ProcState.DONE 
  }
    
  // advance to next action and return if there's more threads to spawn
  def move : (Any,Seq[ProcessThread]) = {
    pre

    // execute action and return continuations: either myself or a bunch of spawns
    val (out, next) = currAct.traverse(nextLink, ctx, currV)
    Audit.recExec  (currAct, currV, out, next.size)
    currV = out

    next map (_.linkState = LinkState.SELECTED)
    
    val res = if (next.size == 1) {
      nextLink = next.firstOption
      nextAct = next.first.z
      this :: Nil // myself continues
    } else {
      nextAct = null 
      if (next.size > 1) 
         next.map (l=>new ProcessThread(l.z, Some(l), ctx, currV)) // // TODO what value to use for start here?
        // note that I'm done in this case and replace myself with these spawns
      else 
        Nil // done - no continuations
    }
    
    post
    (currV,res)
  }
    
  def done () : Boolean = nextAct == null
}

/** 
 * a process instance encapsulates the state of the running workflow 
 * 
 * the state needed to recover is: the definition graph, the graph of states with links to specs and 
 * the values context.
 * 
 * I don't think the history should be kept, unless we're running in some sort of "debug" or "audit" mode.
 */
class Process (val start:WfAct, val ctx:AC, val startV:Any) { 
  var lastV = startV
    
  // the threads in progress these are all in parallel
  var currThreads : Seq[ProcessThread] = new ProcessThread (start, None, ctx, startV) :: Nil
  var oldThreads : Seq[ProcessThread] = Nil // don't know why i keep these

  // progress all threads - this is temp until I get the threading done
  def tick { currThreads = currThreads.flatMap(tick(_)) }
   
  // progress one thread - this would normally happen on its own thread
  def tick (p:ProcessThread) : Seq[ProcessThread] = {
    val (v,n) = p.move
    lastV = v
    n
  }
    
  def done () : Boolean = currThreads.isEmpty
}

/** the engine is more complicated in this case, basically graph traversal */
class Engine { 
  def exec (start:WfAct, ctx:AC, startV:Any) : Any = {
    val p = new Process (start, ctx, startV)       

    preProcess (p)
    
    while (!p.done) p.tick
      
    p.lastV 
  }
  
  def preProcess (p:Process) {
    razie.g.Graphs.foreach (p.start, 
      (l:WfAct,v:Int)=>{},
      (l:WL,v:Int) => l.z match { case a:AndJoin => a addIncoming l ; case _ => }
      )
  }
     
  def checkpoint () {}
}

object Engines {
  def apply () = new Engine()
}


trait Gremlin {
   
}

/** the special activities that control the engine itself - hopefully as few as possible */
object wfeng {
  // TODO start a new process
  def process (root:WfAct) : WfAct = wf.todo
  // TODO start a new sub-process of the current process
  def subprocess (root:WfAct) : WfAct = wf.todo
  
  // TODO start a new sub-process of the current process
  def andjoin (a:WfAct*) : WfAct = wf.todo
}

// and-join state: track incoming links and their values
trait AJState extends WfaState {
  var prev : scala.collection.mutable.HashMap[WL, Any] = null
  
  def addLink (l:WL, v:Any) = {
    if (prev == null)
       prev = razie.Mapi[WL, Any] ()
    prev.put (l, v)
  }
}

/** join parallel branches */
case class AndJoin extends WfAct with AJState { 
  var incoming : List[WL] = Nil // upon starting, the engine will traverse and populate this
  def addIncoming (l:WL) = incoming = List(l) ::: incoming 
  
  /** waiting for all incoming branches and concatenating their values */
  override def traverse (in:AC, v:Any) : (Any,Seq[WL]) = 
     throw new IllegalStateException ("method should never be called")
  override def traverse (from:Option[WL], in:AC, v:Any) : (Any,Seq[WL]) = {
    addLink (from.get, v) // TODO null should bomb indicating andjoin with no branches in...
    if (prev.size == incoming.size) (prev.values map (x => x) toList, glinks) // I'm done
    else (v, Nil) // current thread dies
  }
}
