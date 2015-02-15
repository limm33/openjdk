/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Module;
import java.lang.reflect.Proxy;

public class ProxyAccess {
    private static Class<?> bClass = classForName("p.two.B");
    private static Class<?> cClass = classForName("p.two.internal.C");
    private static Class<?> jClass = classForName("p.one.internal.J");
    private static Class<?> qClass = classForName("p.three.internal.Q");

    private static Module m1 = p.one.I.class.getModule();
    private static Module m2 = p.two.A.class.getModule();
    private static Module m3 = p.three.P.class.getModule();

    static void testProxyClass(Module module, Class<?>... interfaces) {
        ClassLoader ld = module == null ? ProxyAccess.class.getClassLoader() : module.getClassLoader();
        Class<?> proxyClass = Proxy.getProxyClass(ld, interfaces);
        assertTrue(proxyClass.getModule() == module);

        if (module != null) {
            proxyClass = Proxy.getProxyClass(module, interfaces);
            assertTrue(proxyClass.getModule() == module);
        }
    }

    static void testModuleProxyClass(Module module, Class<?>... interfaces) {
        Class<?> proxyClass = Proxy.getProxyClass(module, interfaces);
        assertTrue(proxyClass.getModule() == module);

        Object proxy = Proxy.newProxyInstance(module, handler, interfaces);
        assertTrue(proxy.getClass().getModule() == module);
    }

    static void testInaccessible(Module module, Class<?>... interfaces) {
        try {
            Class<?> proxyClass = Proxy.getProxyClass(module, interfaces);
            expectedIllegalArgumentException();
        } catch (IllegalArgumentException e) {};
    }

    public static void main(String... args) throws Exception {
        Module unnamed = null;
        testProxyClass(unnamed, Runnable.class);
        testProxyClass(unnamed, p.one.I.class);
        testProxyClass(unnamed, p.one.I.class, p.two.A.class);
        testProxyClass(unnamed, p.one.I.class, p.two.A.class, q.U.class);
        testProxyClass(m2, p.two.A.class, bClass);
        testProxyClass(m2, p.two.A.class, bClass, cClass);

        testModuleProxyClass(m1, jClass);
        testModuleProxyClass(m1, jClass, q.U.class);
        testModuleProxyClass(m2, p.two.A.class, cClass);
        testModuleProxyClass(m3, p.two.A.class, qClass);

        testInaccessible(m1, p.one.I.class, p.two.A.class);
        testInaccessible(m3, p.two.A.class, bClass);
        testInaccessible(m3, cClass, qClass);

        // this will add qualified export of sun.invoke from java.base to m1
        testModuleMethodHandle(m1);
        // this will add qualified export of sun.invoke from java.base to unnamed module
        testRunnableMethodHandle(unnamed);
    }

    static void testRunnableMethodHandle(Module module) throws Exception {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(void.class);
        MethodHandle mh = lookup.findStatic(ProxyAccess.class, "runForRunnable", mt);
        Runnable proxy = MethodHandleProxies.asInterfaceInstance(Runnable.class, mh);
        proxy.run();
        Class<?> proxyClass = proxy.getClass();
        assertTrue(proxyClass.getModule() == module);
    }

    static void testModuleMethodHandle(Module module) throws Exception {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(void.class);
        MethodHandle mh = lookup.findStatic(ProxyAccess.class, "runForRunnable", mt);
        p.one.I proxy = MethodHandleProxies.asInterfaceInstance(p.one.I.class, mh);
        proxy.run();
        Class<?> proxyClass = proxy.getClass();
        System.out.format("%s %s%n", proxyClass.getName(), proxyClass.getModule());
        assertTrue(proxyClass.getModule() == module);
    }

    static void runForRunnable() {
        System.out.println("runForRunnable");
    }

    static void assertTrue(boolean expr) {
        if (!expr)
            throw new RuntimeException("Assertion failed");
    }

    static void expectedIllegalArgumentException() {
        throw new RuntimeException("IllegalArgumentException expected");
    }

    private static Class<?> classForName(String cn) {
        try {
            return Class.forName(cn);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private final static InvocationHandler handler = new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method m, Object... params) {
            throw new RuntimeException(m.toString());
        }
    };
}
