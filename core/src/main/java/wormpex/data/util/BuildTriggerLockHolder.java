package wormpex.data.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BuildTriggerLockHolder {
    private static ReentrantReadWriteLock RW_LOCK = new ReentrantReadWriteLock();

    public static ReentrantReadWriteLock.WriteLock getWriteLock() {
        return RW_LOCK.writeLock();
    }

    public static ReentrantReadWriteLock.ReadLock getReadLock() {
        return RW_LOCK.readLock();
    }
}
package wormpex.data.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BuildTriggerLockHolder {
    private static ReentrantReadWriteLock RW_LOCK = new ReentrantReadWriteLock();

    public static ReentrantReadWriteLock.WriteLock getWriteLock() {
        return RW_LOCK.writeLock();
    }


    public static ReentrantReadWriteLock.ReadLock getReadLock() {
        return RW_LOCK.readLock();
    }
}
