package io.tracer.plugin

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class OkHttpBuilderVisitor(nextVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, nextVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return CallSiteInjector(mv)
    }

    class CallSiteInjector(mv: MethodVisitor) : MethodVisitor(Opcodes.ASM9, mv) {
        override fun visitMethodInsn(
            opcode: Int,
            owner: String?,
            name: String?,
            descriptor: String?,
            isInterface: Boolean
        ) {
            // Debug: print candidates
            if (name == "build" && owner?.contains("OkHttpClient") == true) {
                 println("Tracer: Candidate match: owner=$owner, name=$name, desc=$descriptor")
            }

            // Check for: okhttp3/OkHttpClient$Builder.build() -> OkHttpClient
            if (opcode == Opcodes.INVOKEVIRTUAL &&
                owner == "okhttp3/OkHttpClient\$Builder" &&
                name == "build" &&
                descriptor == "()Lokhttp3/OkHttpClient;"
            ) {
                println("Tracer: Injecting interceptor into method call in $owner.$name")
                // Stack before: [Builder]

                val loopStart = org.objectweb.asm.Label()
                val loopEnd = org.objectweb.asm.Label()
                val hasLabel = org.objectweb.asm.Label()

                // Duplicate builder for later use
                visitInsn(Opcodes.DUP) // [Builder, Builder]
                // Call interceptors() on the dup
                visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "okhttp3/OkHttpClient\$Builder",
                    "interceptors",
                    "()Ljava/util/List;",
                    false
                ) // [Builder, List]
                // Get iterator
                visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    "java/util/List",
                    "iterator",
                    "()Ljava/util/Iterator;",
                    true
                ) // [Builder, Iterator]

                visitLabel(loopStart)
                visitInsn(Opcodes.DUP) // [Builder, Iterator, Iterator]
                visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    "java/util/Iterator",
                    "hasNext",
                    "()Z",
                    true
                )
                visitJumpInsn(Opcodes.IFEQ, loopEnd)
                visitInsn(Opcodes.DUP) // [Builder, Iterator, Iterator]
                visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    "java/util/Iterator",
                    "next",
                    "()Ljava/lang/Object;",
                    true
                )
                visitTypeInsn(Opcodes.INSTANCEOF, "io/tracer/TracerInterceptor")
                visitJumpInsn(Opcodes.IFNE, hasLabel)
                visitJumpInsn(Opcodes.GOTO, loopStart)

                // Found existing tracer interceptor
                visitLabel(hasLabel)
                visitInsn(Opcodes.POP) // iterator
                visitInsn(Opcodes.POP) // iterator dup
                visitInsn(Opcodes.POP) // original builder dup
                // Call original build()
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                return

                // No tracer interceptor found; clean up iterator
                visitLabel(loopEnd)
                visitInsn(Opcodes.POP) // iterator
                // Stack: [Builder] (original dup consumed, original builder remains)

                // Inject: .addInterceptor(new TracerInterceptor())
                visitTypeInsn(Opcodes.NEW, "io/tracer/TracerInterceptor")
                visitInsn(Opcodes.DUP)
                visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    "io/tracer/TracerInterceptor",
                    "<init>",
                    "()V",
                    false
                )
                visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "okhttp3/OkHttpClient\$Builder",
                    "addInterceptor",
                    "(Lokhttp3/Interceptor;)Lokhttp3/OkHttpClient\$Builder;",
                    false
                )
                // Stack: [Builder] (returned from addInterceptor)

                // Now call original build()
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            }
        }
    }
}
