/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link ResourceLoader} interface.
 * Used by {@link ResourceEditor}, and serves as base class for
 * {@link org.springframework.context.support.AbstractApplicationContext}.
 * Can also be used standalone.
 *
 * <p>Will return a {@link UrlResource} if the location value is a URL,
 * and a {@link ClassPathResource} if it is a non-URL path or a
 * "classpath:" pseudo-URL.
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see FileSystemResourceLoader
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 *
 *
 * 这个类是 ResourceLoader 接口的默认实现，核心方法为 getResource()
 *
 * 这个类提供 ClassLoader 作为参数的构造器，使用无参构造器时，提供一个默认的 ClassLoader
 * 也可以通过setClassLoader方法设置
 */
public class DefaultResourceLoader implements ResourceLoader {

	@Nullable
	private ClassLoader classLoader;

	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

	private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);


	/**
	 * Create a new DefaultResourceLoader.
	 * <p>ClassLoader access will happen using the thread context class loader
	 * at the time of this ResourceLoader's initialization.
	 * @see java.lang.Thread#getContextClassLoader()
	 *
	 * 默认使用当前线程的 ContextClassLoader
	 */
	public DefaultResourceLoader() {
		this.classLoader = ClassUtils.getDefaultClassLoader();
	}

	/**
	 * Create a new DefaultResourceLoader.
	 * @param classLoader the ClassLoader to load class path resources with, or {@code null}
	 * for using the thread context class loader at the time of actual resource access
	 */
	public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * Specify the ClassLoader to load class path resources with, or {@code null}
	 * for using the thread context class loader at the time of actual resource access.
	 * <p>The default is that ClassLoader access will happen using the thread context
	 * class loader at the time of this ResourceLoader's initialization.
	 *
	 * 设置 ClassLoader
	 */
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Return the ClassLoader to load class path resources with.
	 * <p>Will get passed to ClassPathResource's constructor for all
	 * ClassPathResource objects created by this resource loader.
	 * @see ClassPathResource
	 *
	 * 获取 ClassLoader
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * Register the given resolver with this resource loader, allowing for
	 * additional protocols to be handled.
	 *
	 * 用当前的资源加载器注册给定的(参数)的协议解析器，允许处理额外的协议
	 *
	 * <p>Any such resolver will be invoked ahead of this loader's standard
	 * resolution rules. It may therefore also override any default rules.
	 *
	 * 任意的解析器都会在当前的资源加载器的标准解析规则之前被调用，所以它可以覆盖任何默认规则
	 *
	 * @since 4.3
	 * @see #getProtocolResolvers()
	 *
	 * 注册协议解析器
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		// protocolResolvers 是一个LinkedHashSet
		this.protocolResolvers.add(resolver);
	}

	/**
	 * Return the collection of currently registered protocol resolvers,
	 * allowing for introspection as well as modification.
	 *
	 * 返回当前注册的协议解析器的集合，允许内省和修改
	 *
	 * @since 4.3
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	/**
	 * Obtain a cache for the given value type, keyed by {@link Resource}.
	 * @param valueType the value type, e.g. an ASM {@code MetadataReader}
	 * @return the cache {@link Map}, shared at the {@code ResourceLoader} level
	 * @since 5.0
	 *
	 * 获取给定值类型的缓存
	 *
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
		return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
	}

	/**
	 * Clear all resource caches in this resource loader.
	 * @since 5.0
	 * @see #getResourceCache
	 *
	 * 清除当前资源加载器中所有的缓存资源
	 *
	 */
	public void clearResourceCaches() {
		this.resourceCaches.clear();
	}


	/**
	 * 资源加载器的核心方法
	 * 根据给定的路径返回相应的Resource
	 * 本类的两个核心子类都没有覆盖此方法，所以ResourceLoader的资源加载策略就在本类中
	 *
	 * 本方法只能根据给定的一个location返回一个Resource，需要加载多个资源时，可循环调用
	 * 或者使用ResourceLoader的扩展
	 * @see ResourcePatternResolver
	 */
	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");
		// 首先通过协议解析器来加载，成功则返回Resource
		for (ProtocolResolver protocolResolver : this.protocolResolvers) {
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}
		// 如果协议解析器没有加载成功，则继续后续逻辑
		// 如果location以 "/" 开头，就构造ClassPathContextResource类型资源并返回
		if (location.startsWith("/")) {
			return getResourceByPath(location);
		}
		// 如果location以 "classpath:" 开头，就构造ClassPathResource并返回，构造资源时通过getClassLoader()获取当前的classloader
		else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		// 如果不是上面两种情况
		else {
			try {
				// Try to parse the location as a URL...
				// 把location当做网络位置，尝试构造一个URL,如果不成功，就抛出MalformedURLException异常，
				// 并委托给getResourceByPath(String path)实现资源定位加载，
				// 也就是构造ClassPathContextResource类型资源并返回
				URL url = new URL(location);
				return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
			}
			catch (MalformedURLException ex) {
				// No URL -> resolve as resource path.
				return getResourceByPath(location);
			}
		}
	}

	/**
	 * Return a Resource handle for the resource at the given path.
	 * <p>The default implementation supports class path locations. This should
	 * be appropriate for standalone implementations but can be overridden,
	 * e.g. for implementations targeted at a Servlet container.
	 * @param path the path to the resource
	 * @return the corresponding Resource handle
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 *
	 * 返回给定路径上的资源句柄
	 *
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * ClassPathResource that explicitly expresses a context-relative path
	 * through implementing the ContextResource interface.
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

	/**
	 * DefaultResourceLoader 加载资源的具体策略演示，代码如下（该示例参考《Spring 揭秘》 P89）
	 * 这里的演示不必关心文件是否真实存在，
	 * 因为ResourceLoader接口已说明：getResource方法不保证资源存在，需要调用Resource.exists()方法判断资源存在性
	 * @param args
	 */
	public static void main(String[] args) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();

		Resource fileResource1 = resourceLoader.getResource("D:/Users/chenming673/Documents/spark.txt");
		// fileResource1 is FileSystemResource:false
		/* 这里我们可能预期fileResource1是FileSystemResource资源类型，但其实是ClassPathResource类型，
		 * 这是因为这里会进入gerResource类型的else,并在异常处理中构造并返回一个ClassPathResource
		 * 从这里我们可以感觉到DefaultResourceLoader.getResourceByPath(path)方法的处理其实不太恰当，
		 * 此时可以使用FileSystemResourceLoader，它覆盖了getResourceByPath方法，可以从文件系统加载资源，并以FileSystemResource类型返回
		 *
		 * 参见 fileResource3
		 */
		System.out.println("fileResource1 is FileSystemResource:" + (fileResource1 instanceof FileSystemResource));

		Resource fileResource2 = resourceLoader.getResource("/Users/chenming673/Documents/spark.txt");
		// fileResource2 is ClassPathResource:true
		System.out.println("fileResource2 is ClassPathResource:" + (fileResource2 instanceof ClassPathResource));

		Resource urlResource1 = resourceLoader.getResource("file:/Users/chenming673/Documents/spark.txt");
		// urlResource1 is UrlResource:true
		System.out.println("urlResource1 is UrlResource:" + (urlResource1 instanceof UrlResource));

		Resource urlResource2 = resourceLoader.getResource("http://www.baidu.com");
		// urlResource1 is urlResource:true
		System.out.println("urlResource1 is urlResource:" + (urlResource2 instanceof  UrlResource));

		resourceLoader = new FileSystemResourceLoader();
		Resource fileResource3 = resourceLoader.getResource("D:/Users/chenming673/Documents/spark.txt");
		// fileResource3 is FileSystemResource:true
		System.out.println("fileResource3 is FileSystemResource:" + (fileResource3 instanceof FileSystemResource));
	}

}
