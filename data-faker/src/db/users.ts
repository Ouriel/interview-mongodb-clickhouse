import { name, internet } from 'faker'
import { InsertOneWriteOpResult, InsertWriteOpResult } from 'mongodb'

import { db } from './db'
import { Events, insertOneLog } from './logs'
import fakeDate from '../fakeDate'

interface IUser {
  name: string
  password: string
  createdAt: Date
  updatedAt: Date
}

export async function insertUsers(
  n = 1
): Promise<InsertOneWriteOpResult | InsertWriteOpResult> {
  const Users = db.collection('Users')

  const users = [...new Array(n)].map(
    (): IUser => ({
      name: name.findName(),
      password: internet.password(),
      createdAt: fakeDate.now.toDate(),
      updatedAt: fakeDate.now.toDate(),
    })
  )

  const op = await (n === 1
    ? Users.insertOne(users[0])
    : Users.insertMany(users))

  const ids = n === 1 ? [op.insertedId] : Object.values(op.insertedIds)

  await Promise.all(ids.map(id => insertOneLog(id, Events.Signin)))

  return op
}
