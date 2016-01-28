package quasar.fs

import quasar.Predef._
import quasar._, RenderTree.ops._
import quasar.fp._

import pathy.{Path => PPath}, PPath._
import scalaz._, Scalaz._

/** Interpreter that just records a log of the actions that are performed. */
object TraceFS {
  val FsType = FileSystemType("trace")

  type Trace[A] = Writer[Vector[RenderedTree], A]

  def qfTrace(paths: Map[ADir, Set[PathName]]) = new (QueryFile ~> Trace) {
    import QueryFile._

    def ls(dir: ADir) =
      paths.getOrElse(dir, Set())

    def apply[A](qf: QueryFile[A]): Trace[A] =
      WriterT.writer((Vector(qf.render),
        qf match {
          case ExecutePlan(lp, out) => (Vector.empty, \/-(out))
          case EvaluatePlan(lp)     => (Vector.empty, \/-(ResultHandle(0)))
          case More(handle)         => \/-(Vector.empty)
          case Close(handle)        => ()
          case Explain(lp)          => (Vector.empty, \/-(ExecutionPlan(FsType, lp.toString)))
          case ListContents(dir)    => \/-(ls(dir))
          case FileExists(file)     => ls(fileParent(file)).contains(fileName(file).right).right
        }))
  }

  def rfTrace = new (ReadFile ~> Trace) {
    import ReadFile._

    def apply[A](rf: ReadFile[A]): Trace[A] =
      WriterT.writer((Vector(rf.render),
        rf match {
          case Open(file, off, lim) => \/-(ReadHandle(file, 0))
          case Read(handle)         => \/-(Vector.empty)
          case Close(handle)        => ()
        }))
  }

  def wfTrace = new (WriteFile ~> Trace) {
    import WriteFile._

    def apply[A](wf: WriteFile[A]): Trace[A] =
      WriterT.writer((Vector(wf.render),
        wf match {
          case Open(file)           => \/-(WriteHandle(file, 0))
          case Write(handle, chunk) => Vector.empty
          case Close(handle)        => ()
        }))
  }

  def mfTrace = new (ManageFile ~> Trace) {
    import ManageFile._

    def apply[A](mf: ManageFile[A]): Trace[A] =
      WriterT.writer((Vector(mf.render),
        mf match {
          case Move(scenario, semantics) => \/-(())
          case Delete(path)              => \/-(())
          case TempFile(near) =>
            \/-(refineType(near).fold(ι, fileParent) </> file("tmp"))
        }))
  }

  def traceFs(paths: Map[ADir, Set[PathName]]): FileSystem ~> Trace =
    interpretFileSystem[Trace](qfTrace(paths), rfTrace, wfTrace, mfTrace)

  def traceInterp[A](t: Free[FileSystem, A], paths: Map[ADir, Set[PathName]]): (Vector[RenderedTree], A) = {
    new free.Interpreter(traceFs(paths)).interpret(t).run
  }
}
