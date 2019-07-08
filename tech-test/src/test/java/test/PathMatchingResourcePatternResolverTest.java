package test;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @author: zhangyafeng1
 * @Date 2019-07-03 16:20:40
 */
public class PathMatchingResourcePatternResolverTest {
	private static final String[] CLASSES_IN_CORE_IO_SUPPORT =
			new String[] {"EncodedResource.class", "LocalizedResourceHelper.class",
					"PathMatchingResourcePatternResolver.class", "PropertiesLoaderSupport.class",
					"PropertiesLoaderUtils.class", "ResourceArrayPropertyEditor.class",
					"ResourcePatternResolver.class", "ResourcePatternUtils.class"};

	private static final String[] TEST_CLASSES_IN_CORE_IO_SUPPORT =
			new String[] {"PathMatchingResourcePatternResolverTests.class"};

	private static final String[] CLASSES_IN_REACTIVESTREAMS =
			new String[] {"Processor.class", "Publisher.class", "Subscriber.class", "Subscription.class"};

	private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
	public static void main(String[] args) throws IOException {
		PathMatchingResourcePatternResolverTest test = new PathMatchingResourcePatternResolverTest();
		// =========================================================================================================
//		test.resolver.getResources("xx**:**/*.xy");
		// =========================================================================================================
//		Resource[] resources =
//				test.resolver.getResources("org/springframework/core/io/support/PathMatchingResourcePatternResolverTests.class");
		// =========================================================================================================
//		Resource[] resources = test.resolver.getResources("classpath*:org/springframework/core/io/sup*/*.class");
//		// Have to exclude Clover-generated class files here,
//		// as we might be running as part of a Clover test run.
//		List<Resource> noCloverResources = new ArrayList<>();
//		for (Resource resource : resources) {
//			if (!resource.getFilename().contains("$__CLOVER_")) {
//				noCloverResources.add(resource);
//			}
//		}
//		resources = noCloverResources.toArray(new Resource[noCloverResources.size()]);
		// =========================================================================================================
//		Resource[] resources = test.resolver.getResources("classpath:org/reactivestreams/*.class");
		// =========================================================================================================
		Resource[] resources = test.resolver.getResources("classpath*:*.dtd");
		System.out.println(resources);
		// =========================================================================================================
		// =========================================================================================================
	}

}
