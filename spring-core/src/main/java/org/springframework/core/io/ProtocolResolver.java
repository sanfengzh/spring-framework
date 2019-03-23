/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * A resolution strategy for protocol-specific resource handles.
 *
 * <p>Used as an SPI for {@link DefaultResourceLoader}, allowing for
 * custom protocols to be handled without subclassing the loader
 * implementation (or application context implementation).
 *
 * 用户自定义的协议资源解决策略，
 * 它允许用户自定义资源加载协议，而不需要继承 ResourceLoader 的子类。
 * 在介绍 Resource 时，提到如果要实现自定义 Resource，我们只需要继承 DefaultResource 即可，
 * 但是有了 ProtocolResolver 后，我们不需要直接继承 DefaultResourceLoader，改为实现 ProtocolResolver 接口也可以实现自定义的 ResourceLoader
 *
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultResourceLoader#addProtocolResolver
 */
@FunctionalInterface
public interface ProtocolResolver {

	/**
	 * Resolve the given location against the given resource loader
	 * if this implementation's protocol matches.
	 *
	 * 如果这个实现的协议匹配，就根据给定的资源加载器加载给定的location
	 *
	 * 这个方法的实现需要用户自己实现，并通过DefaultResourceLoader.addProtocolResolver()将自定义实现的ProtocolResolver实例注册进去
	 *
	 * @param location the user-specified resource location   用户指定的资源位置
	 * @param resourceLoader the associated resource loader   相关的资源加载器
	 * @return a corresponding {@code Resource} handle if the given location
	 * matches this resolver's protocol, or {@code null} otherwise
	 */
	@Nullable
	Resource resolve(String location, ResourceLoader resourceLoader);

}
