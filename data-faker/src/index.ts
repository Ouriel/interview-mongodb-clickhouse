import fakeDate from './fakeDate'
import logger from './logger'
import { db, connect } from './db/db'
import { insertUsers } from './db/users'
import { Events, insertOneLog } from './db/logs'

const TICK_INTERVAL = parseInt(process.env.TICK_INTERVAL, 10)

const conf = {
  initialUsersNb: 10,
  signupGrowthPercentage: 2,
  signinPercentage: 10,
}

/**
 * init: reset DB, fill with some users and start the loop process
 */
;(async function init() {
  try {
    await connect()
  } catch (err) {
    // handle reconnection
    console.log(err.message, 'Trying to reconnect')
    await new Promise(resolve => setTimeout(() => resolve(), 1000))
    init()
    return
  }

  fakeDate.init()

  // clean
  await db.collection('Users').removeMany({})
  await db.collection('Logs').removeMany({})

  // insert first users
  await insertUsers(conf.initialUsersNb)

  tick()
})()

// loop
async function tick() {
  const start = Date.now()

  await updateData()

  const tickDuration = Date.now() - start
  fakeDate.increment()
  setTimeout(() => tick(), TICK_INTERVAL - tickDuration)
}

async function updateData() {
  try {
    const Users = db.collection('Users')
    const Logs = db.collection('Logs')
    const usersCount = await Users.countDocuments()
    const logsCount = await Logs.countDocuments()
    logger(
      fakeDate.now.format('DD/MM/YYYY'),
      `Users: ${usersCount}, Signins: ${logsCount}`
    )

    /*
      NEW USERS INSERTIONS
    */
    const nbOfNewUsers = Math.round(
      (usersCount * conf.signupGrowthPercentage) / 100
    )
    await insertUsers(nbOfNewUsers || 1)

    /*
      SIGNIN LOGS
    */
    const nbOfSignins = Math.round((usersCount * conf.signinPercentage) / 100)
    await Promise.all(
      [...new Array(nbOfSignins)].map(async () => {
        // Pick a random user
        const index = Math.floor(Math.random() * usersCount)
        const user = await Users.findOne(
          {},
          { projection: { _id: 1 }, skip: index }
        )
        // signin
        await insertOneLog(user._id, Events.Signin)
      })
    )
  } catch (err) {
    console.log(err)
    process.exit()
  }
}
