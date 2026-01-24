package wormpex.data.util;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public final class ProxyUserQueue {
    private static final int size = 1;
    private LinkedBlockingDeque<ProxyUser> users = new LinkedBlockingDeque<>(size + 2);

    public ProxyUser getLastUser() {
        return users.isEmpty() ? null : users.getLast();
    }

    public String getLastUserName() {
        ProxyUser lastUser = getLastUser();
        return lastUser == null ? null : lastUser.getName();
    }

    public boolean isEmpty() {
        return users.isEmpty();
    }

    public ProxyUserQueue add(ProxyUser proxyUser) {
        users.add(proxyUser);
        expire();
        return this;
    }

    private void expire() {
        while (users.size() > size) {
            users.poll();
        }
    }
}
package wormpex.data.util;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public final class ProxyUserQueue {
    private static final int size = 1;
    private LinkedBlockingDeque<ProxyUser> users = new LinkedBlockingDeque<>(size + 2);


    public ProxyUser getLastUser() {
        return users.isEmpty() ? null : users.getLast();
    }
    public String getLastUserName() {
        ProxyUser lastUser = getLastUser();
        return lastUser == null ? null: lastUser.getName();
    }

    public boolean  isEmpty(){
        return users.isEmpty();
    }

    public ProxyUserQueue add(ProxyUser proxyUser) {
        users.add(proxyUser);
        expire();
        return this;

    }

    private void expire() {
        while (users.size() > size) {
            users.poll();
        }
    }

    public static void main(String[] args) {
        final ProxyUserQueue proxyUserQueue = new ProxyUserQueue();
        Thread t1 = new Thread() {
            /**
             * If this thread was constructed using a separate
             * <code>Runnable</code> run object, then that
             * <code>Runnable</code> object's <code>run</code> method is called;
             * otherwise, this method does nothing and returns.
             * <p>
             * Subclasses of <code>Thread</code> should override this method.
             *
             * @see #start()
             * @see #stop()
             * @see #Thread(ThreadGroup, Runnable, String)
             */
            @Override
            public void run() {
                while (true) {
                    ProxyUser lastUser = proxyUserQueue.getLastUser();
                    System.out.println(lastUser);

                    try {
                        TimeUnit.MICROSECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("*** " + proxyUserQueue.users.size());

                }
            }
        };
        Thread t2 = new Thread() {
            /**
             * If this thread was constructed using a separate
             * <code>Runnable</code> run object, then that
             * <code>Runnable</code> object's <code>run</code> method is called;
             * otherwise, this method does nothing and returns.
             * <p>
             * Subclasses of <code>Thread</code> should override this method.
             *
             * @see #start()
             * @see #stop()
             * @see #Thread(ThreadGroup, Runnable, String)
             */
            @Override
            public void run() {
                while (true) {
                    ProxyUser proxyUser = new ProxyUser(System.currentTimeMillis() + " ");
                    proxyUserQueue.add(proxyUser);
                    System.out.println("add " + proxyUser);
                    try {
                        TimeUnit.MICROSECONDS.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t2.start();
        t1.start();

    }
}
