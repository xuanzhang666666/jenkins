package wormpex.data.util;

import java.util.concurrent.LinkedBlockingDeque;

public final class ProxyUserQueue {
    private static final int SIZE = 1;
    private final LinkedBlockingDeque<ProxyUser> users = new LinkedBlockingDeque<>(SIZE + 2);

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
        while (users.size() > SIZE) {
            users.poll();
        }
    }
}
