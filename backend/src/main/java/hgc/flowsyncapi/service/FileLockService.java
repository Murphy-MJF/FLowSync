package hgc.flowsyncapi.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class FileLockService {

    private final Map<String, LockInfo> locks = new ConcurrentHashMap<>();

    public static class LockInfo {
        String lockKey;          // repoOwner/repoName:branch:filePath
        Long currentUserId;
        String currentUserName;
        LocalDateTime lockTime;
        Queue<WaitingUser> waitingQueue = new ConcurrentLinkedQueue<>();
    }

    public static class WaitingUser {
        Long userId;
        String userName;
        LocalDateTime joinTime;

        WaitingUser(Long userId, String userName) {
            this.userId = userId;
            this.userName = userName;
            this.joinTime = LocalDateTime.now();
        }
    }

    /** 尝试获取文件锁。返回 null 表示获取成功；返回等待信息表示需排队 */
    public AcquireResult acquire(String repoOwner, String repoName, String branch,
                                  String filePath, Long userId, String userName) {
        String key = buildKey(repoOwner, repoName, branch, filePath);
        LockInfo lock = locks.computeIfAbsent(key, k -> {
            LockInfo li = new LockInfo();
            li.lockKey = key;
            return li;
        });

        synchronized (lock) {
            // 无人占用 → 直接获取
            if (lock.currentUserId == null) {
                lock.currentUserId = userId;
                lock.currentUserName = userName;
                lock.lockTime = LocalDateTime.now();
                return AcquireResult.acquired();
            }

            // 同一用户 → 直接返回持有
            if (lock.currentUserId.equals(userId)) {
                return AcquireResult.acquired();
            }

            // 已在队列中 → 返回队列位置
            for (WaitingUser w : lock.waitingQueue) {
                if (w.userId.equals(userId)) {
                    int position = new ArrayList<>(lock.waitingQueue).indexOf(w) + 1;
                    return AcquireResult.queued(position, lock.currentUserName);
                }
            }

            // 加入等待队列
            lock.waitingQueue.add(new WaitingUser(userId, userName));
            int position = lock.waitingQueue.size();
            return AcquireResult.queued(position, lock.currentUserName);
        }
    }

    /** 释放文件锁，并通知下一位等待者 */
    public WaitingUser release(String repoOwner, String repoName, String branch,
                                String filePath, Long userId) {
        String key = buildKey(repoOwner, repoName, branch, filePath);
        LockInfo lock = locks.get(key);
        if (lock == null) return null;

        synchronized (lock) {
            if (!userId.equals(lock.currentUserId)) return null;
            lock.currentUserId = null;
            lock.currentUserName = null;
            lock.lockTime = null;

            // 取下一个等待者
            WaitingUser next = lock.waitingQueue.poll();
            if (next != null) {
                lock.currentUserId = next.userId;
                lock.currentUserName = next.userName;
                lock.lockTime = LocalDateTime.now();
            }

            if (lock.currentUserId == null && lock.waitingQueue.isEmpty()) {
                locks.remove(key);
            }
            return next;
        }
    }

    /** 查询文件锁状态 */
    public Map<String, Object> status(String repoOwner, String repoName, String branch, String filePath) {
        String key = buildKey(repoOwner, repoName, branch, filePath);
        LockInfo lock = locks.get(key);
        Map<String, Object> result = new HashMap<>();
        if (lock == null || lock.currentUserId == null) {
            result.put("locked", false);
            return result;
        }
        result.put("locked", true);
        result.put("currentUserId", lock.currentUserId);
        result.put("currentUserName", lock.currentUserName);
        result.put("lockTime", lock.lockTime != null ? lock.lockTime.toString() : null);
        result.put("queueLength", lock.waitingQueue.size());
        return result;
    }

    private String buildKey(String owner, String repo, String branch, String path) {
        return owner + "/" + repo + ":" + branch + ":" + path;
    }

    public static class AcquireResult {
        public boolean acquired;
        public int queuePosition;
        public String currentHolder;

        static AcquireResult acquired() {
            AcquireResult r = new AcquireResult();
            r.acquired = true;
            return r;
        }
        static AcquireResult queued(int pos, String holder) {
            AcquireResult r = new AcquireResult();
            r.acquired = false;
            r.queuePosition = pos;
            r.currentHolder = holder;
            return r;
        }
    }
}
