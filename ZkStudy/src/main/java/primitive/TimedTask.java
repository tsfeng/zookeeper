package primitive;

import org.apache.zookeeper.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author admin
 * @title: TimedTask
 * @projectName zookeeper
 * @description: TODO
 * @date 2019/9/1813:38
 */
public class TimedTask {

    private static final String zkServerUrl = "127.0.0.1:12181,127.0.0.1:12182,127.0.0.1:12183";

    private static final String lockPath = "/primitive/lock";

    private String machineName;

    public TimedTask(String machineName) {
        this.machineName = machineName;
    }

    public void go() {
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            /**
             * 连接 zooKeeper 集群
             */
            ZooKeeper zooKeeper = new ZooKeeper(zkServerUrl, 5000, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    latch.countDown();
                }
            });
            latch.await();
            System.out.println("zooKeeper 建立连接成功" + zooKeeper);

            /**
             * 原生
             * 尝试成为 master
             */
            toBeAMaster(zooKeeper);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toBeAMaster(ZooKeeper zooKeeper) {
        zooKeeper.create(lockPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int i, String s, Object o, String s1) {
                if (i == KeeperException.Code.OK.intValue()) {
                    System.out.println(machineName + " 拿到锁");
                    try {
                        TimeUnit.SECONDS.sleep(3);
                        zooKeeper.delete(lockPath, -1);
                        System.out.println(machineName + " 宕机");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (i == KeeperException.Code.NODEEXISTS.intValue()) {
                    System.out.println(machineName + " 没有拿到锁，等待");
                    try {
                        zooKeeper.exists(lockPath, new Watcher() {
                            @Override
                            public void process(WatchedEvent watchedEvent) {
                                if (watchedEvent.getType().equals(Event.EventType.NodeDeleted)) {
                                    System.out.println(machineName + " 监控到节点删除");
                                    toBeAMaster(zooKeeper);
                                }
                            }
                        });
                    } catch (KeeperException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("其他状态码：" + i);
                }
            }
        }, "ctx_data");
    }


    public static void main(String[] args) throws InterruptedException {
        IntStream.rangeClosed(1, 5)
                .mapToObj(index -> "机器" + index)
                .map(TimedTask::new)
                .map(task -> (Runnable) () -> task.go())
                .map(Thread::new)
                .forEach(Thread::start);
        System.out.println("over");
        TimeUnit.SECONDS.sleep(16);
    }
}
