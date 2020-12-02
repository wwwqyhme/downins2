package me.qyh.instd4j.parser.auth;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScannerTwoFactorCodeProvider implements TwoFactorCodeProvider {
    @Override
    public Future<String> getCodeFuture(String username) {

        return new Future<>() {
            private String code;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return code != null;
            }

            @Override
            public String get() throws InterruptedException, ExecutionException {
                Scanner scan = new Scanner(System.in);
                System.out.println("please input two factor code");
                code = scan.nextLine();
                return code;
            }

            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return get();
            }
        };
    }
}
