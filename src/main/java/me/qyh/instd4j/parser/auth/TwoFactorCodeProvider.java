package me.qyh.instd4j.parser.auth;

import java.util.concurrent.Future;

public interface TwoFactorCodeProvider {

    Future<String> getCodeFuture(String username);

}
