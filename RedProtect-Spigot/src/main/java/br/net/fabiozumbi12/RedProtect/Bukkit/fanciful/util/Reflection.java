/*
 * Copyright (c) 2019 - @FabioZumbi12
 * Last Modified: 25/04/19 07:02
 *
 * This class is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any
 *  damages arising from the use of this class.
 *
 * Permission is granted to anyone to use this class for any purpose, including commercial plugins, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 * 1 - The origin of this class must not be misrepresented; you must not claim that you wrote the original software. If you
 * use this class in other plugins, an acknowledgment in the plugin documentation would be appreciated but is not required.
 * 2 - Altered source versions must be plainly marked as such, and must not be misrepresented as being the original class.
 * 3 - This notice may not be removed or altered from any source distribution.
 *
 * Esta classe é fornecida "como está", sem qualquer garantia expressa ou implícita. Em nenhum caso os autores serão
 * responsabilizados por quaisquer danos decorrentes do uso desta classe.
 *
 * É concedida permissão a qualquer pessoa para usar esta classe para qualquer finalidade, incluindo plugins pagos, e para
 * alterá-lo e redistribuí-lo livremente, sujeito às seguintes restrições:
 * 1 - A origem desta classe não deve ser deturpada; você não deve afirmar que escreveu a classe original. Se você usar esta
 *  classe em um plugin, uma confirmação de autoria na documentação do plugin será apreciada, mas não é necessária.
 * 2 - Versões de origem alteradas devem ser claramente marcadas como tal e não devem ser deturpadas como sendo a
 * classe original.
 * 3 - Este aviso não pode ser removido ou alterado de qualquer distribuição de origem.
 */

package br.net.fabiozumbi12.RedProtect.Bukkit.fanciful.util;

import br.net.fabiozumbi12.RedProtect.Core.helpers.CoreUtil;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A class containing static utility methods and caches which are intended as reflective conveniences.
 * Unless otherwise noted, upon failure methods will return {@code null}.
 */
public final class Reflection {

    /**
     * Stores loaded classes from the {@code net.minecraft.server} package.
     */
    private static final Map<String, Class<?>> _loadedNMSClasses = new HashMap<>();
    /**
     * Stores loaded classes from the {@code org.bukkit.craftbukkit} package (and subpackages).
     */
    private static final Map<String, Class<?>> _loadedOBCClasses = new HashMap<>();
    private static final Map<Class<?>, Map<String, Field>> _loadedFields = new HashMap<>();
    /**
     * Contains loaded methods in a cache.
     * The map maps [types to maps of [method names to maps of [parameter types to method instances]]].
     */
    private static final Map<Class<?>, Map<String, Map<ArrayWrapper<Class<?>>, Method>>> _loadedMethods = new HashMap<>();
    private static String _versionString;

    private Reflection() {

    }

    /**
     * Gets the version string from the package name of the CraftBukkit server implementation.
     * This is needed to bypass the JAR package name changing on each update.
     *
     * @return The version string of the OBC and NMS packages, <em>including the trailing dot</em>.
     */
    public synchronized static String getVersion() {
        if (_versionString == null) {
            if (Bukkit.getServer() == null) {
                // The server hasn't started, static initializer call?
                return null;
            }
            String name = Bukkit.getServer().getClass().getPackage().getName();
            _versionString = name.substring(name.lastIndexOf('.') + 1) + ".";
        }

        return _versionString;
    }

    /**
     * Gets a {@link Class} object representing a type contained within the {@code net.minecraft.server} versioned package.
     * The class instances returned by this method are cached, such that no lookup will be done twice (unless multiple threads are accessing this method simultaneously).
     *
     * @param className The name of the class, excluding the package, within NMS.
     * @return The class instance representing the specified NMS class, or {@code null} if it could not be loaded.
     */
    public synchronized static Class<?> getNMSClass(String className) {
        if (_loadedNMSClasses.containsKey(className)) {
            return _loadedNMSClasses.get(className);
        }

        String fullName = "net.minecraft.server." + getVersion() + className;
        Class<?> clazz;
        try {
            clazz = Class.forName(fullName);
        } catch (Exception e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
            _loadedNMSClasses.put(className, null);
            return null;
        }
        _loadedNMSClasses.put(className, clazz);
        return clazz;
    }

    /**
     * Gets a {@link Class} object representing a type contained within the {@code org.bukkit.craftbukkit} versioned package.
     * The class instances returned by this method are cached, such that no lookup will be done twice (unless multiple threads are accessing this method simultaneously).
     *
     * @param className The name of the class, excluding the package, within OBC. This name may contain a subpackage name, such as {@code inventory.CraftItemStack}.
     * @return The class instance representing the specified OBC class, or {@code null} if it could not be loaded.
     */
    public synchronized static Class<?> getOBCClass(String className) {
        if (_loadedOBCClasses.containsKey(className)) {
            return _loadedOBCClasses.get(className);
        }

        String fullName = "org.bukkit.craftbukkit." + getVersion() + className;
        Class<?> clazz;
        try {
            clazz = Class.forName(fullName);
        } catch (Exception e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
            _loadedOBCClasses.put(className, null);
            return null;
        }
        _loadedOBCClasses.put(className, clazz);
        return clazz;
    }

    /**
     * Attempts to get the NMS handle of a CraftBukkit object.
     * <p>
     * The only match currently attempted by this method is a retrieval by using a parameterless {@code getHandle()} method implemented by the runtime type of the specified object.
     * </p>
     *
     * @param obj The object for which to retrieve an NMS handle.
     * @return The NMS handle of the specified object, or {@code null} if it could not be retrieved using {@code getHandle()}.
     */
    public synchronized static Object getHandle(Object obj) {
        try {
            return getMethod(obj.getClass(), "getHandle").invoke(obj);
        } catch (Exception e) {
            CoreUtil.printJarVersion();
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves a {@link Field} instance declared by the specified class with the specified name.
     * Java access modifiers are ignored during this retrieval. No guarantee is made as to whether the field
     * returned will be an instance or static field.
     * <p>
     * A global caching mechanism within this class is used to store fields. Combined with synchronization, this guarantees that
     * no field will be reflectively looked up twice.
     * </p>
     * <p>
     * If a field is deemed suitable for return, {@link Field#setAccessible(boolean) setAccessible} will be invoked with an argument of {@code true} before it is returned.
     * This ensures that callers do not have to check or worry about Java access modifiers when dealing with the returned instance.
     * </p>
     *
     * @param clazz The class which contains the field to retrieve.
     * @param name  The declared name of the field in the class.
     * @return A field object with the specified name declared by the specified class.
     * @see Class#getDeclaredField(String)
     */
    public synchronized static Field getField(Class<?> clazz, String name) {
        Map<String, Field> loaded;
        if (!_loadedFields.containsKey(clazz)) {
            loaded = new HashMap<>();
            _loadedFields.put(clazz, loaded);
        } else {
            loaded = _loadedFields.get(clazz);
        }
        if (loaded.containsKey(name)) {
            // If the field is loaded (or cached as not existing), return the relevant value, which might be null
            return loaded.get(name);
        }
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            loaded.put(name, field);
            return field;
        } catch (Exception e) {
            // Error loading
            CoreUtil.printJarVersion();
            e.printStackTrace();
            // Cache field as not existing
            loaded.put(name, null);
            return null;
        }
    }

    /**
     * Retrieves a {@link Method} instance declared by the specified class with the specified name and argument types.
     * Java access modifiers are ignored during this retrieval. No guarantee is made as to whether the field
     * returned will be an instance or static field.
     * <p>
     * A global caching mechanism within this class is used to store method. Combined with synchronization, this guarantees that
     * no method will be reflectively looked up twice.
     * </p>
     * <p>
     * If a method is deemed suitable for return, {@link Method#setAccessible(boolean) setAccessible} will be invoked with an argument of {@code true} before it is returned.
     * This ensures that callers do not have to check or worry about Java access modifiers when dealing with the returned instance.
     * </p>
     * <p>
     * This method does <em>not</em> search superclasses of the specified type for methods with the specified signature.
     * Callers wishing this behavior should use {@link Class#getDeclaredMethod(String, Class...)}.
     *
     * @param clazz The class which contains the method to retrieve.
     * @param name  The declared name of the method in the class.
     * @param args  The formal argument types of the method.
     * @return A method object with the specified name declared by the specified class.
     */
    public synchronized static Method getMethod(Class<?> clazz, String name,
                                                Class<?>... args) {
        if (!_loadedMethods.containsKey(clazz)) {
            _loadedMethods.put(clazz, new HashMap<>());
        }

        Map<String, Map<ArrayWrapper<Class<?>>, Method>> loadedMethodNames = _loadedMethods.get(clazz);
        if (!loadedMethodNames.containsKey(name)) {
            loadedMethodNames.put(name, new HashMap<>());
        }

        Map<ArrayWrapper<Class<?>>, Method> loadedSignatures = loadedMethodNames.get(name);
        ArrayWrapper<Class<?>> wrappedArg = new ArrayWrapper<>(args);
        if (loadedSignatures.containsKey(wrappedArg)) {
            return loadedSignatures.get(wrappedArg);
        }

        for (Method m : clazz.getMethods())
            if (m.getName().equals(name) && Arrays.equals(args, m.getParameterTypes())) {
                m.setAccessible(true);
                loadedSignatures.put(wrappedArg, m);
                return m;
            }
        loadedSignatures.put(wrappedArg, null);
        return null;
    }

}
