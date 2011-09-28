/** ____    __    ____  ____  ____,,___     ____  __  __  ____
 *  (  _ \  /__\  (_   )(_  _)( ___)/ __)   (  _ \(  )(  )(  _ \           Read
 *   )   / /(__)\  / /_  _)(_  )__) \__ \    )___/ )(__)(  ) _ <     README.txt
 *  (_)\_)(__)(__)(____)(____)(____)(___/   (__)  (______)(____/    LICENSE.txt
 */
package razie

import java.lang.IllegalStateException
import razie.base.{ ActionContext => AC }
import razie.gremlins.act.WfCase2ap
import razie.gremlins.act.WfGroupBy
import razie.gremlins.act.WfSliceMap
import razie.gremlins.act.WfCase1
import razie.gremlins.act.WfCase2
import razie.gremlins.act.WfCase2a
import razie.gremlins.act.WfCase2p
import razie.gremlins.act.WfCaseAny1
import razie.gremlins.act.WfCases1
import razie.gremlins.act.WfDynIf
import razie.gremlins.act.WfDynIfa
import razie.gremlins.act.WfDynPar
import razie.gremlins.act.WfDynSeq
import razie.gremlins.act.WfFlatten
import razie.gremlins.act.WfFoldLeft
import razie.gremlins.act.WfGuard1
import razie.gremlins.act.WfIf
import razie.gremlins.act.WfLabelInsert
import razie.gremlins.act.WfMap
import razie.gremlins.act.WfMatch1
import razie.gremlins.act.WfPar
import razie.gremlins.act.WfScala
import razie.gremlins.act.WfScalaV0
import razie.gremlins.act.WfScalaV1
import razie.gremlins.act.WfScalaV1u
import razie.gremlins.act.WfSort
import razie.gremlins.act.WfeScala
import razie.gremlins.act.WfeScalaV1
import razie.gremlins.WFunc
import razie.gremlins.$Expr
import razie.gremlins.WfActivity
import razie.gremlins.WfExec
import razie.gremlins.WfaCollector
import razie.wf
import scala.actors.Actor
import scala.actors.Actor._

/** actor flows
 *
 *  NOTE you should not mix wfa with either wf or wfs...
 *
 *  The ENTIRE body is executed once, like the constructor of a class. only specially marked statements
 *  become nodes in the workflow...those prefixed by a keyword in this object.
 *
 *  The workflow then built can be run at will, once (or many times at your risk)!
 *  You can build it again and again with a def instead of val.
 *
 *  As a general rule, each node gets an input value and returns an output value. The "par" will create a list.
 *
 *  @author Razvan Cojocaru
 */
class wfa {
  /** build a lazy seq node in a scala workflow. Note that the body won't be executed until the workflow is started,
   *  unless is marked as strict - see strict below.
   *
   *  The body of the seq is executed when the seq node is started. It will be collecting all the activities built inside
   *  the body. All the activities collected will then be executed in sequence.
   *
   *  If no activity is built in the body, you end up with a plain scala code execution...
   *
   *  You can mix and embedd seq and par on multiple levels.
   */
  def seq(body: => Unit): WfActivity = wrap (strict { new WfDynSeq(new WfeScala(body), false) }) {
    s => 
      val se = s.asInstanceOf[WfDynSeq]
    new WfaaSeq (se.collector.collect{case a:WfaaActor=>a}, s)
  }

  /** build a lazy par node in a scala workflow. Note that the body won't be executed until the workflow is started,
   *  unless is marked as strict - see strict below.
   *
   *  The body of the par is executed when the par node is started. It will be collecting all the activities built inside
   *  the body. All the activities collected will then be started in parallel. Their results will be joined back and
   *  returned as a list. In essence, this acts like a fork/join on as many threads as activities are built inside the
   *  body.
   *
   *  If no activity is built in the body, you end up with a plain scala code execution...
   *
   *  You can mix and embedd seq and par on multiple levels.
   */
  def par(body: => Unit): WfActivity = wrap (strict { new WfDynSeq(new WfeScala(body), false) }) {
    s => 
      val se = s.asInstanceOf[WfDynSeq]
    new WfaaPar (se.collector.collect{case a:WfaaActor=>a}, s)
  }

  /** create a leaf activity from a partial function. */
  def branchReactor(body: PartialFunction[Any, Unit]) =
    new WfaaActorAA (
    actor {
      loop {
        react (body)
      }
    })

  /** create a leaf activity from a partial function. */
  def reactor[A,B](body: PartialFunction[A, B]) =
    new WfaaReactor (body)

  /** interpret this workflow definition right now, not later */
  private def strict(f: => WfActivity): WfActivity = WfaCollector.flagged ("strict") (f)

  private def wrap(f: WfActivity)(gen: WfActivity => WfaaActor): WfActivity = gen(f)

}

class WfaaPar (la:Seq[WfaaActor], a:WfActivity) extends WfaaActorW(a)
class WfaaSeq (la:Seq[WfaaActor], a:WfActivity) extends WfaaActorW(a)

class WfaaReactor[A,B] (body: PartialFunction[A, B]) extends WfaaActorA {
  override val mkActor = 
    actor {
      loop {
        react {
          case x:A if (body.isDefinedAt(x)) => {
            val ret = body.apply(x)
            nextA collect {case a:WfaaActorA=>a} map (_.mkActor ! ret)
          }
        }
      }
    }
}

class WfaaActorW(val a: WfActivity) extends WfaaActor {
  override def apply(in: AC, v: Any): Any = a.run (v)
}

class WfaaActorAA(a: Actor) extends WfaaActorA {
  override val mkActor = a
}

abstract class WfaaActorA extends WfaaActor {
  val mkActor : Actor
}

class WfaaActor extends WfScalaV1((x) => x) {
  var nextA : List[WfaaActor] = Nil // TODO should this just be a 
}

// this stuff not tested yet
object wfta extends wfa {

  //----------------- base activitities

  /** create leaf activity containing scala code. It cannot contain more activities */
  def w(body: => Unit): WfActivity = new WfScala(() => {
    WfaCollector.cantCollect ("async") { body }
  })

  /** create leaf activity containing scala code. It cannot contain more activities */
  def w(body: Any => Any): WfActivity = new WfScalaV1((x) => {
    WfaCollector.cantCollect ("sync") { body(x) }
  })

  /** create leaf activity containing scala code. It cannot contain more activities */
  def later(body: Any => Any): WfActivity = w(body)

  /** create a leaf activity from a partial function. */
  def matchLater[B](body: PartialFunction[Any, B]) =
    new WfScalaV1((x) => if (body.isDefinedAt(x)) body(x) else x)

  //  def apply(f: => Unit) = w(f)
  def apply(f: Any => Any) = w(f)

  /** assign name in context to the last value produced by the previous activity */
  def assign(name: String) = wf.assign(name, wf.$0)

  def $(name: String) = wf.$(name)
  def $0 = wf.$0

  //------------------- call

  /** call a sub-process with a different startup value than $0
   *
   *  @param e an expression evaluated in the context of THIS workflow
   */
  def call(e: $Expr)(f: => WfActivity): WfActivity = new WfsCall(e, f) // TODO not tested
  def call(f: => WfActivity): WfActivity = call($0)(f) // TODO not tested

  def scall(a: => WfActivity) = noCollect { a }.run ("kk")

  /** scala code with input and return values */
  class WfsCall(val e: $Expr, val a: WfActivity) extends WfScalaV1((x) => x) {
    // TODO this runs the sub-workflow, which is expensive, should instead bind it and fork through it
    // remember the same wf may be shared at the same time by many instances/wokflows?
    override def apply(in: AC, v: Any): Any = a.run (e.apply(in, v))
  }

  /** send to an actor and wait reply
   *
   *  TODO implement asynchronously
   */
  def act(e: $Expr)(a: => scala.actors.Actor): WfActivity = new WfScalaV1((x) => x) {
    override def apply(in: AC, v: Any): Any = a !? (e.apply(in, v))
  }
  /** send $0 to an actor and wait reply */
  def act(a: => scala.actors.Actor): WfActivity = act ($0)(a) // TODO test

  //--------------------- simulating the let! from F# syntax

  class LetBang(name: String) {
    def !(f: => WfActivity) = wfs.let (name) (f)
  }

  def let = new LetBang("var-" + razie.g.GRef.uid)

  def ilet(name: String)(f: => WfActivity) = {
    // this is collected...but not in a par
    if (WfaCollector.who.map(_.isInstanceOf[WfPar]).getOrElse(false))
      throw new IllegalStateException("You can't use a let in a par{} block")
    val v = new LetBangVar(name)
    (wf seq noCollect {
      Seq(f, wf.wrap(new LetBangVarAssign(v)))
    },
      v)
  }

  def let(name: String)(f: => WfActivity) = ilet (name) (f)._2

  //----------------- if

  type FB = WFunc[Boolean]
  type Cond1 = Any => Boolean

  implicit def wc0(cond: Boolean): WFunc[Boolean] = new WFunc[Boolean] { override def apply(in: AC, v: Any) = cond }
  def wc1(cond: Any => Boolean): WFunc[Boolean] = new WFunc[Boolean] { override def apply(in: AC, v: Any) = cond(v) }
  def wcAnd(cond: Any => Boolean)(wf: WFunc[Boolean]): WFunc[Boolean] = new WFunc[Boolean] { override def apply(in: AC, v: Any) = cond(v) && wf.apply(in, v) }
  def wcnot(wf: WFunc[Boolean]): WFunc[Boolean] = new WFunc[Boolean] { override def apply(in: AC, v: Any) = !wf.apply(in, v) }

  /** a lazy if-else construct. The activities built on the if and else branches are
   *  collected inside a wfs.seq
   */
  def wif(cond: FB)(f: => Unit) = new WfDynIf(cond, f)
  def wif(cond: Cond1)(f: => Unit) = new WfDynIf(wc1(cond), f)

  //----------------------- list stuff

  def map[A, B](f: A => B) = new WfMap[A, B](f)
  def imap[A, B](a: Int, b: Int)(f: A => B) = new WfSliceMap[A, B](new WfeScalaV1(x => (a, b)))(f)

  def foldLeft[T](zero: T)(plus: (T, T) => T) = new WfFoldLeft[T](zero)(plus)
  def groupBy[T, K](g: T => K) = new WfGroupBy[T, K](g)
  def sort[T](lt: (T, T) => Boolean) = new WfSort[T](lt)

  implicit val linkFactory = (x, y) => new razie.gremlins.WfLink(x, y)

  class LString(name: String) {
    def label(f: => WfActivity) = new WfLabelInsert(name) --> noCollect { f }
  }
  implicit def toLString(label: String) = new LString(label)

  def wforeach[T](f: => WfActivity) = "wforeach" label noCollect {
    val (vorigA, vorig) = wfs.ilet ("var-" + razie.g.GRef.uid) (wf.nop)
    val (va, v) = wfs.ilet ("var-" + razie.g.GRef.uid) (new WfScalaV0(() => 0))
    val head = wf.label ("head", wf.nop)
    val loop = wf.label ("loop", wf.stop(1))
    val done = wf.label ("done", wf.stop(1))
    val body = wf seq Seq (
      // swap list with current element
      "swapElement" label w { x => vorig.get.asInstanceOf[List[T]] (v.get.asInstanceOf[Int]) },
      f,
      "next" label w { x => v.get.asInstanceOf[Int] + 1 }, // increment counter
      wf.wrap(new LetBangVarAssign(v)),
      "restore" label w { x => vorig.get } // restore the list
      )
    body --> loop
    val i = wfs strict {
      new WfDynIfa(
        wc1(x => { v.get.asInstanceOf[Int] < vorig.get.asInstanceOf[List[T]].size }),
        body) welse done
    }

    // after the if is built, redirect end to head
    loop --> head

    //    Seq(va, va1, head, i)
    vorigA --| va --| (head --> i)
  }

  //================================================== collecting control

  /** shortcut for nocolelct -  don't collect these activities. Collection is a hack for the workflow DSL */
  def noCollect[T](f: => T): T = WfaCollector.noCollect (f)

  /** do not collect the contents, just the result */
  def collectOne(f: => WfActivity): WfActivity = {
    val body = noCollect {
      f
    }
    WfaCollector.current.map { _ collect body }
    body
  }

  /** do not collect the contents, just the result */
  def resultOf(f: => WfActivity): WfActivity = collectOne (f)

  /** interpret this workflow definition right now, not later */
  def strict(f: => WfActivity): WfActivity = WfaCollector.flagged ("strict") (f)

  //================================================== looping

  val max: Int = 100

  /** build the equivalent construct of an actor.loopWhile: a loop reading from a queue, until the body's result evaluates the condition to false.
   *  This is implemented as a simple repeatUntil.
   *
   *  NOTE that the body needs to respect the reset() because it will loop
   *
   *  NOTE if you want to have body behave like an actor (not read from default but wait on a channel), then use a channel yourself, see the PingPong parallel sample, in samples
   *
   *  @param cond is a boolean expression evaluated on each message, while true the actor runs, when false it stops
   *  @param body the body of the actor - note this is not collecting, just the result will be the actual body
   */
  def loopWhile(e: Any => Boolean)(body: => WfActivity): WfActivity =
    wf.loopWhile (wc1(x => e(x))) (body)

  /** repeat the body as long as the condition is true. condition tested up-front
   *
   *  @param cond is a boolean expression evaluated with the current value
   *  @param body the body of the actor - note this is not collecting, just the result will be the actual body
   */
  def doWhile(e: Any => Boolean)(body: => WfActivity): WfActivity =
    doWhile (wc1(x => e(x))) (body)

  /** repeat the body as long as the condition is true. condition tested up-front
   *
   *  @param cond is a boolean expression evaluated with the current value
   *  @param body the body of the actor - note this is not collecting, just the result will be the actual body
   */
  def doWhile(e: WFunc[Boolean])(body: => WfActivity): WfActivity = wf.doWhile(e)(body)

  /** repeat the body as long as the condition is true. condition tested at the end
   *
   *  @param cond is a boolean expression evaluated with the current value
   *  @param body the body of the actor - note this is not collecting, just the result will be the actual body
   */
  def repeatUntil(e: Any => Boolean)(body: => WfActivity): WfActivity =
    repeatUntil (wc1(x => e(x))) (body)

  /** repeat the body as long as the condition is true. condition tested at the end
   *
   *  @param cond is a boolean expression evaluated with the current value
   *  @param body the body of the actor - note this is not collecting, just the result will be the actual body
   */
  def repeatUntil(e: WFunc[Boolean])(body: => WfActivity): WfActivity = wf.repeatUntil(e)(body)

  //================================== maps and stuff

  def wsmap[A, B](branches: Int)(f: A => B) = "wsmap" label noCollect {
    val p = seq {
      par {
        for (i <- 0 until branches)
          new WfSliceMap[A, B](new WfeScalaV1(x => {
            val s = x.asInstanceOf[List[A]].size
            val step = s / branches
            val max = s / branches
            (i * step, min ((i + 1) * step, s))
          }))(f)
      }
      // sort the lists
      w { x =>
        {
          val v = x.asInstanceOf[List[(Int, List[B])]]
          val r = v.sortWith ((a, b) => a._1 < b._1)
          r map (_._2)
        }
      }
      new WfFlatten[B]
    }
    p
  }

  private def min(a: Int, b: Int): Int = if (a < b) a else b

  //  simple map branch
  def wimap[T](f: => WfActivity) = "wsimap" label noCollect {
    val (vorigA, vorig) = wfs.ilet ("var-" + razie.g.GRef.uid) (wf.nop)
    val (va, v) = wfs.ilet ("var-" + razie.g.GRef.uid) (new WfScalaV0(() => 0))
    val (vac, vc) = wfs.ilet ("var-" + razie.g.GRef.uid) (new WfScalaV0(() => List()))
    val head = wf.label ("head", wf.nop)
    val loop = wf.label ("loop", wf.stop(1))
    val done = wf.label ("done", wf.stop(1))
    val body = wf seq Seq (
      // swap list with current element
      "swapElement" label w { x => vorig.get.asInstanceOf[List[T]] (v.get.asInstanceOf[Int]) },
      f,
      "collect" label w { x => x :: vc.get.asInstanceOf[List[T]] },
      wf.wrap(new LetBangVarAssign(vc)),
      "next" label w { x => v.get.asInstanceOf[Int] + 1 }, // increment counter
      wf.wrap(new LetBangVarAssign(v)),
      "restore" label w { x => vorig.get } // restore the list
      )
    body --> loop
    val i = new WfDynIfa(
      wc1(x => { v.get.asInstanceOf[Int] < vorig.get.asInstanceOf[List[T]].size }),
      body) welse done

    val result = "result" label w { x => vc.get }

    // after the if is built, redirect end to head
    loop --> head

    //    Seq(va, va1, head, i)
    vorigA --| va --| vac --| (head --> i) // --| result
  }

  def wmap[T](branches: Int)(f: => WfActivity) = "wmap" label noCollect {
    val (va1, v1) = wfs.ilet ("var-" + razie.g.GRef.uid) (wf.nop)
    val (va, v) = wfs.ilet ("var-" + razie.g.GRef.uid) (new WfScalaV0(() => 0))
    val (vac, vc) = wfs.ilet ("var-" + razie.g.GRef.uid) (new WfScalaV0(() => List()))
    val head = wf.label ("head", wf.nop)
    val loop = wf.label ("loop", wf.stop(1))
    val done = wf.label ("done", wf.stop(1))
    val body = wf seq Seq (
      // swap list with current element
      "1" label w { x => v1.get.asInstanceOf[List[T]] (v.get.asInstanceOf[Int]) },
      f,
      "2" label w { x => v.get.asInstanceOf[Int] + 1 }, // increment counter
      wf.wrap(new LetBangVarAssign(v)),
      "3" label w { x => v1.get } // restore the list
      )
    body --> loop
    val i = new WfDynIfa(
      wc1(x => { v.get.asInstanceOf[Int] <= wf.$0.asInstanceOf[List[T]].size }),
      body) welse done

    // after the if is built, redirect end to head
    loop --> head

    //    Seq(va, va1, head, i)
    va1 --| va --| vac --| (head --> i)
  }

}

object wfa extends wfa
