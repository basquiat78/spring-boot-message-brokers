package io.basquiat.global.provider

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 * ApplicationContextAware 를 이용해서 ApplicationContext 를 제공하는 제공자.
 * inner holder 패턴으로 static 처럼 사용.
 */
@Component
class ApplicationContextProvider : ApplicationContextAware {
    private object ApplicationContextHolder {
        val innerPrivateResource = InnerPrivateResource()
    }

    /**
     * 내부에서만 ApplicationContext 에 접근 가능하게
     */
    private class InnerPrivateResource {
        var context: ApplicationContext? = null
            private set

        fun setContext(context: ApplicationContext) {
            this.context = context
        }
    }

    override fun setApplicationContext(context: ApplicationContext) {
        ApplicationContextHolder.innerPrivateResource.setContext(context)
    }

    companion object {
        fun getApplicationContext(): ApplicationContext? = ApplicationContextHolder.innerPrivateResource.context
    }
}