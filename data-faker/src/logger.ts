export default (...args) => {
  if (process.env.QUIET !== 'false') return
  console.log(...args)
}
