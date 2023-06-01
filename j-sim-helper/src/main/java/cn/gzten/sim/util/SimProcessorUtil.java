package cn.gzten.sim.util;

import cn.gzten.exception.SimRequestError;
import cn.gzten.util.HttpParamUtil;
import cn.gzten.util.JsonUtil;
import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import org.eclipse.jetty.io.ByteBufferInputStream;

import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.util.List;

public class SimProcessorUtil {
    public static void doTypeMapping(List<CodeBlock> codes, VariableElement p, String paramName, boolean mandatory) {
        if (mandatory) {
            codes.add(CodeBlock.of("if (!queryParams.containsKey($S)) { throw new $T($S);}\n",
                    paramName, SimRequestError.class, "Not found parameter: %s".formatted(paramName)));
        }

        if (ClassName.get(p.asType()).equals(TypeName.get(String.class))) {
            var template = "var _$N = queryParams.getValue($S);\n";
            codes.add(CodeBlock.of(template, paramName, paramName));
        } else if (ClassName.get(p.asType()).equals(TypeName.get(Integer.class))) {
            var template = "var _$N = Integer.parseInt(queryParams.getValue($S));\n";
            codes.add(CodeBlock.of(template, paramName, paramName));
        } else if (ClassName.get(p.asType()).equals(TypeName.get(Long.class))) {
            var template = "var _$N = Long.parseLong(queryParams.getValue($S));\n";
            codes.add(CodeBlock.of(template, paramName, paramName));
        } else {
            var errMsg = "Error at compiling: does not support type: " + ClassName.get(p.asType()).toString();
            System.err.println(errMsg);
            throw new RuntimeException(errMsg);
        }

    }

    public static void doRequestBody(List<CodeBlock> codes, VariableElement p, String paramName) {
        codes.add(CodeBlock.of("$T _$N = null;\n", p.asType(), paramName));
        codes.add(CodeBlock.of("try(var ins = new $T(request.read().getByteBuffer())) {\n", ByteBufferInputStream.class));
        if (ClassName.get(p.asType()).equals(TypeName.get(String.class))) {
            var template = "  _$N = queryParams.getValue($S);\n";
            codes.add(CodeBlock.of(template, paramName, paramName));
        } else if (ClassName.get(p.asType()).equals(TypeName.get(Integer.class))) {
            var template = "  _$N = Integer.parseInt(queryParams.getValue($S));\n";
            codes.add(CodeBlock.of(template, paramName, paramName));
        } else if (ClassName.get(p.asType()).equals(TypeName.get(Long.class))) {
            var template = "  _$N = Long.parseLong(queryParams.getValue($S));\n";
            codes.add(CodeBlock.of(template, paramName, paramName));
        } else {
            var template = "  _$N = $T.toObject(ins, new $T<$T>(){});\n";
            codes.add(CodeBlock.of(template, paramName, JsonUtil.class, TypeToken.class, p.asType()));
        }
        codes.add(CodeBlock.of("} catch (RuntimeException | $T e) {\n", IOException.class));
        codes.add(CodeBlock.of("  e.printStackTrace();\n"));
        codes.add(CodeBlock.of("""
                  $T.returnError(response, callback, 400, "Request body parsing error: %s".formatted(e.getMessage()));\n
                """, HttpParamUtil.class));
        codes.add(CodeBlock.of("}\n"));
    }
}
