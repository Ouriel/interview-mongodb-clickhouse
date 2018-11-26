import { InsertOneWriteOpResult } from 'mongodb'

import { db } from './db'
import fakeDate from '../fakeDate'

export enum Events {
  Signin = 'sign-in',
  ChangePassword = 'change-password',
}

interface ILog {
  userId: Object
  event: Events
  success: Boolean
  date: Date
}

export async function insertOneLog(
  userId: Object,
  event: Events,
  success = true
): Promise<InsertOneWriteOpResult> {
  const Logs = db.collection('Logs')

  const log: ILog = {
    userId,
    event,
    success,
    date: fakeDate.now.toDate(),
  }

  return Logs.insertOne(log)
}
