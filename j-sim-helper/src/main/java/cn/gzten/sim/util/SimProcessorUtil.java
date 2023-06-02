package cn.gzten.sim.util;

import cn.gzten.annotation.PathVar;
import cn.gzten.annotation.QueryParam;
import cn.gzten.annotation.RequestBody;
import cn.gzten.exception.SimRequestError;
import cn.gzten.util.JsonUtil;
import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import org.eclipse.jetty.util.StringUtil;

import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SimProcessorUtil {
    public static void doTypeMapping(List<CodeBlock> codes, VariableElement p, String paramName, boolean mandatory) {
        if (mandatory) {
            codes.add(CodeBlock.of("if (!queryParams.containsKey($S)) { throw new $T($S);}\n",
                    paramName, SimRequestError.class, "Not found parameter: %s".formatted(paramName)));
        }

        // get it as String first.
        codes.add(CodeBlock.of("String _$N_str = queryParams.getValue($S);\n", paramName, paramName));

        var paramType = ClassName.get(p.asType());
        if (paramType.equals(TypeName.get(String.class))) {
            var template = "String _$N = _$N_str;\n";
            codes.add(CodeBlock.of(template, paramName, paramName));
        } else if (paramType.equals(TypeName.INT) || paramType.equals(TypeName.INT.box())) {
            var template = "Integer _$N = $T.isBlank(_$N_str)?null:Integer.parseInt(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.LONG) || paramType.equals(TypeName.LONG.box())) {
            var template = "Long _$N = $T.isBlank(_$N_str)?null:Long.parseLong(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.BOOLEAN) || paramType.equals(TypeName.BOOLEAN.box())) {
            var template = "Boolean _$N = $T.isBlank(_$N_str)?null:Boolean.parseBoolean(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.BYTE) || paramType.equals(TypeName.BYTE.box())) {
            var template = "Byte _$N = $T.isBlank(_$N_str)?null:Byte.parseByte(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.SHORT) || paramType.equals(TypeName.SHORT.box())) {
            var template = "Short _$N = $T.isBlank(_$N_str)?null:Short.parseShort(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.CHAR) || paramType.equals(TypeName.CHAR.box())) {
            var template = "Char _$N = $T.isBlank(_$N_str)?null:_$N_str.charAt(0);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.FLOAT) || paramType.equals(TypeName.FLOAT.box())) {
            var template = "Float _$N = $T.isBlank(_$N_str)?null:Float.parseFloat(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        }  else if (paramType.equals(TypeName.DOUBLE) || paramType.equals(TypeName.DOUBLE.box())) {
            var template = "Double _$N = $T.isBlank(_$N_str)?null:Double.parseDouble(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else {
            var errMsg = "Error at compiling: does not support type: " + ClassName.get(p.asType()).toString();
            System.err.println(errMsg);
            throw new RuntimeException(errMsg);
        }

    }

    public static void doRequestBody(List<CodeBlock> codes, VariableElement p, String paramName) {
        var typeName = TypeName.get(p.asType());
        if (typeName.isPrimitive()) {
            typeName = typeName.box();
        }

        codes.add(CodeBlock.of("$T _$N = null;\n", typeName, paramName));
        codes.add(CodeBlock.of("try(var ins = ctx.getRequestBodyAsStream()) {\n"));
        if (typeName.equals(TypeName.get(String.class)) || typeName.isBoxedPrimitive()) {
            codes.add(CodeBlock.of("  String _$N_str = new String(ins.readAllBytes());\n"));
            if (typeName.equals(TypeName.get(String.class))) {
                var template = "  _$N = _$N_str;\n";
                codes.add(CodeBlock.of(template, paramName, paramName));
            } else if (typeName.equals(TypeName.INT.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Integer.parseInt(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.LONG.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Long.parseLong(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            }else if (typeName.equals(TypeName.BOOLEAN.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Boolean.parseBoolean(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.BYTE.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Byte.parseByte(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.SHORT.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Short.parseShort(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.CHAR.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:_$N_str.charAt(0);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.FLOAT.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Float.parseFloat(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            }  else if (typeName.equals(TypeName.DOUBLE.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Double.parseDouble(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else {
                var errMsg = "SimProcessorUtil.doRequestBody: Error at compiling: does not support type: " + typeName;
                System.err.println(errMsg);
                throw new RuntimeException(errMsg);
            }
        } else {
            var template = "  _$N = $T.toObject(ins, new $T<$T>(){});\n";
            codes.add(CodeBlock.of(template, paramName, JsonUtil.class, TypeToken.class, p.asType()));
        }
        codes.add(CodeBlock.of("} catch (RuntimeException | $T e) {\n", IOException.class));
        codes.add(CodeBlock.of("  e.printStackTrace();\n"));
        codes.add(CodeBlock.of("""
                  ctx.returnWith(400, "Request body parsing error: %s".formatted(e.getMessage()));
                """));
        codes.add(CodeBlock.of("}\n"));
    }

    public static MethodParameterResult prepareMethodParameters(List<? extends VariableElement> methodParams) {
        var result = new MethodParameterResult();
        var queryParamDefined = false;
        for (var p : methodParams){
            var qp = p.getAnnotation(QueryParam.class);
            var paramName = p.getSimpleName().toString();
            if (qp != null) {
                if (!queryParamDefined) {
                    result.codes.add(CodeBlock.of("var queryParams = ctx.getQueryParams();\n"));
                    queryParamDefined = true;
                }
                if (StringUtil.isNotBlank(qp.value())) {
                    paramName = qp.value();
                }
                result.paramNames.add("_" + paramName);

                SimProcessorUtil.doTypeMapping(result.codes, p, paramName, qp.required());

                continue;
            }
            var pv = p.getAnnotation(PathVar.class);
            if (pv != null) {
                if (StringUtil.isNotBlank(pv.value())) {
                    paramName = pv.value();
                }
                result.paramNames.add("_" + paramName);
                continue;
            }

            var rb = p.getAnnotation(RequestBody.class);
            if (rb != null) {
                result.paramNames.add("_" + paramName);
                SimProcessorUtil.doRequestBody(result.codes, p, paramName);
                continue;
            }
        }

        return result;
    }

    public static class MethodParameterResult {
        public List<String> paramNames = new LinkedList<>();
        public List<CodeBlock> codes = new LinkedList<>();
    }
}
