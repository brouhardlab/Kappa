import joblib as jb


def parallelized(fn, args, args_type='single', progress_bar=False, n_jobs=-1, tqdm_args=None, **joblib_args):
  """A wrapper function to parallelize computation easily.

  It requires [joblib](https://github.com/joblib/joblib) for
  parallelization and [tqdm](https://github.com/tqdm/tqdm)
  to show a progress bar.

  Args:
    fn: Python function, the function to parallelize.
    args: list, the list of arguments to pass to the function `fn`.
    args_type: string, how arguments should be passed to `fn`.
      If 'single', values in args are passed as a single argument.
      If 'list', values are passed as positional arguments (using *).
      If 'dict', values are passed as keyword arguments (using **).
    progress_bar: bool, display a progress bar during computation.
    n_jobs: int, number of jobs running in parallel. Set to -1, to
      use all availalbe CPUs and 1 to disable parallelization. See
      `joblib.Parallel` for details.
    tqdm_args: dict, arguments for the tqdm progress bar. See
      https://github.com/tqdm/tqdm#parameters for details.
    joblib_args: keyword arguments for `joblib.Parallel`.

  Example:
  ```python
  import anamic
  import numpy as np

  def long_fn(arg1, arg2):
    import time
    time.sleep(1)
    return arg1 + arg2

  args = np.random.randint(0, 50, size=(10, 2))
  results = anamic.utils.parallelized(long_fn, args, args_type='list', progress_bar=True, n_jobs=2)
  ```
  """

  if not tqdm_args:
    tqdm_args = {}

  if progress_bar:
    from tqdm.auto import tqdm
    def progress_fn(x): return tqdm(x, **tqdm_args)
  else:
    def progress_fn(x): return x

  parallel_fn = jb.delayed(fn)

  if args_type == 'single':
    executor_args = [parallel_fn(arg) for arg in args]
  elif args_type == 'list':
    executor_args = [parallel_fn(*arg) for arg in args]
  elif args_type == 'dict':
    executor_args = [parallel_fn(**arg) for arg in args]
  else:
    mess = "`args_type` must be in ['single', 'list', 'dict']"
    raise ValueError(mess)

  executor = jb.Parallel(n_jobs=n_jobs, **joblib_args)
  def executor_fn(x): return executor(progress_fn(x))
  return executor_fn(executor_args)
