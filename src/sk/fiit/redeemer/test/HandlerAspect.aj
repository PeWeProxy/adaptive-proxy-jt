package sk.fiit.redeemer.test;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;

import rabbit.handler.BaseHandler;
import rabbit.handler.ImageHandler;
import rabbit.handler.MultiPartHandler;
import rabbit.httpio.WebConnectionResourceSource;

import rabbit.nio.NioHandler;
import rabbit.nio.MultiSelectorNioHandler;

import rabbit.zip.GZipUnpacker;
import rabbit.zip.GZipPacker;

public aspect HandlerAspect {
	
	public HandlerAspect() {
		/*new Thread(new Runnable() {
			
			@Override
			public synchronized void run() {
				try {
					wait(1000);
					synchronized (outWindows) {
						for (DebugWindow window : outWindows.values()) {
							window.hideWindow();
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();*/
	}
	
	pointcut inHandlersOfInterest() : within(BaseHandler+) && !within(ImageHandler) && !within(MultiPartHandler)
										&& !this(ImageHandler) && !this(MultiPartHandler);
	
	pointcut initInHandlerCode(BaseHandler handler) : inHandlersOfInterest() && initialization(BaseHandler+.new(..)) && this(handler);
	
	pointcut methodInHandlerCode(BaseHandler handler) : initInHandlerCode(handler)
						|| (inHandlersOfInterest() && execution(* *.*(..)) && !execution(String *.toString(..)) && (this(handler)));
	
	pointcut methodInHandlersInnerClassCode(BaseHandler handler) : inHandlersOfInterest() && execution(* *.*(..))
						&& !this(BaseHandler+) && this(Object) && cflow(methodInHandlerCode(handler));
	
	pointcut variableInHandlerCode(BaseHandler handler, Object o) : inHandlersOfInterest() && set(* *.*) && this(handler) && within(BaseHandler+) && args(o);
	
	pointcut bufferOps(BaseHandler handler, ByteBuffer buf) : inHandlersOfInterest() && call(* ByteBuffer+.*(..)) && this(handler) && target(buf);
	
	pointcut bufferChangingOps(BaseHandler handler, ByteBuffer buf) : bufferOps(handler, buf) && (call(* ByteBuffer+.array(..)) 
													|| call(* ByteBuffer+.get(..))
													|| call(* ByteBuffer+.position(int)));
	
	pointcut waitForRead(BaseHandler handler, WebConnectionResourceSource readHandler) : cflow(methodInHandlerCode(handler)) && call(void NioHandler+.waitForRead(..))
						&& args(*,readHandler) && target(MultiSelectorNioHandler);
	
	pointcut zippersMethods(BaseHandler handler) : cflow(methodInHandlerCode(handler))
						&& (execution(* GZipUnpacker.*(..)) || execution(* GZipPacker.*(..)));
	
	Map<BaseHandler, WebConnectionResourceSource> lastReadHandlers = new HashMap<BaseHandler, WebConnectionResourceSource>();
	
	// Vypis registrovania poziadavky citat data zo spojenia
	before(BaseHandler handler, WebConnectionResourceSource readHandler): waitForRead(handler,readHandler) {
		if (!(readHandler instanceof WebConnectionResourceSource))
			return;
		WebConnectionResourceSource lastReadHandler = lastReadHandlers.get(handler);
		if (lastReadHandler == null || lastReadHandler != readHandler)
			lastReadHandlers.put(handler, readHandler);
		if (lastReadHandler != null && lastReadHandler != readHandler)
			printOut(handler, "ERROR - "+readHandler.toString(), debugTextType.WAITFORREAD, 8);
		else
			printOut(handler, readHandler.toString(), debugTextType.WAITFORREAD, 0);
	}
	
	// Vypis novych hodnot premennych
	before(BaseHandler handler, Object o): variableInHandlerCode(handler, o) {
		Signature sig = thisJoinPoint.getSignature();
		String varName = sig.getDeclaringType().getSimpleName()+"."+sig.getName();
		printOut(handler,varName+" = "+o,debugTextType.VARIABLE,
				varName.length());
	}
	
	// Vypis PRED metodami meniacimi stav ByteBuffer objektu
	before(BaseHandler handler, ByteBuffer buf) : bufferChangingOps(handler, buf) {
		String methodName = thisJoinPoint.getSignature().getName();
		printOut(handler,methodName+"()\t"+buf.toString(),debugTextType.BUFFER_OP,
				methodName.length()+2);
	}
	
	// Vypis PO metodach nemeniacich stav ByteBuffer objektu
	after(BaseHandler handler, ByteBuffer buf) : bufferOps(handler, buf) {
		String methodName = thisJoinPoint.getSignature().getName();
		printOut(handler,methodName+"()\t"+buf.toString(),debugTextType.BUFFER_USED,
				methodName.length()+2);
	}
	
	Map<BaseHandler, DebugWindow> outWindows = new HashMap<BaseHandler, DebugWindow>();
	Map<BaseHandler, Boolean> factoryInstances = new HashMap<BaseHandler, Boolean>();
	Map<BaseHandler, String> offsets = new HashMap<BaseHandler, String>();
	
	// Zachytenie inicializacie objektov triedy BaseHandler a podtried
	before(BaseHandler handler): initInHandlerCode(handler) {
		if (!outWindows.containsKey(handler)) {
			synchronized (outWindows) {
				boolean isFactoryInstance = (thisJoinPoint.getArgs().length==0);
				factoryInstances.put(handler, isFactoryInstance);
				outWindows.put(handler, DebugWindow.newWindow(handler.toString(),isFactoryInstance));
			}
			offsets.put(handler, "");
		}
	}
	
	Map<Object, BaseHandler> innerInstances = new HashMap<Object, BaseHandler>();
	
	void printMethod(JoinPoint jPoint, BaseHandler handler) {
		Signature sig = jPoint.getSignature();
		Object[] args = jPoint.getArgs();
		//System.out.println(sig.getName());
		StringBuilder sb = new StringBuilder();
		sb.append(sig.getDeclaringType().getSimpleName());
		sb.append(".");
		sb.append(sig.getName());
		sb.append("()");
		int boldLenght = sb.length();
		if (args.length != 0) {
			sb.append('\t');
			sb.append(args[0]);
			if (args.length > 1)
				for (int i = 0; i < args.length; i++) {
					sb.append(", ");
					sb.append(args[i]);
				}
		}
		printOut(handler, sb.toString(), debugTextType.METHOD,boldLenght);
		offsets.put(handler, offsets.get(handler)+ " | ");
	}
	
	// Vypis PRED vykonavanim metody objektu triedy BaseHandler a podtried
	before(BaseHandler handler): methodInHandlerCode(handler) {
		printMethod(thisJoinPoint, handler);
	}
	
	// Vypis PRED vykonavanim metody objektu triedy vnorenej triede BaseHandler a podtriedam
	before(BaseHandler handler): methodInHandlersInnerClassCode(handler) {
		printMethod(thisJoinPoint, handler);
	}
	
	// Vypis PRED vykonavanim metody objektu packera alebo unpackera
	before(BaseHandler handler): zippersMethods(handler) {
		printMethod(thisJoinPoint, handler);
	}
	
	// PO vykonavani metody objektu triedy BaseHandler a podtried
	after(BaseHandler handler): methodInHandlerCode(handler) {
		offsets.put(handler, offsets.get(handler).substring(3));
	}

	// PO vykonavani metody objektu triedy vnorenej triede BaseHandler a podtriedam
	after(BaseHandler handler): methodInHandlersInnerClassCode(handler) {
		offsets.put(handler, offsets.get(handler).substring(3));
	}
	
	// PO vykonavani metody objektu packera alebo unpackera
	after(BaseHandler handler): zippersMethods(handler) {
		offsets.put(handler, offsets.get(handler).substring(3));
	}
	
	//before(BaseHandler handler): 
	
	enum debugTextType { METHOD, VARIABLE, BUFFER_USED, BUFFER_OP, WAITFORREAD }
	
	void printOut(BaseHandler handler, String text, debugTextType type, int headEnd) {
		DebugWindow window = outWindows.get(handler);
		String heading = null;
		Color color = null;
		if (type == debugTextType.METHOD) {
			heading = "MET: ";
			color = Color.BLUE;
		} else if (type == debugTextType.VARIABLE) {
			heading = "VAR: ";
			color = Color.MAGENTA;
		} else if (type == debugTextType.BUFFER_USED) {
			heading = "BUF: ";
			color = Color.GREEN;
		} else if (type == debugTextType.BUFFER_OP) {
			heading = "BUF PRED: ";
			color = Color.RED;
		} else {
			heading = "WAIT FOR READ: ";
			color = Color.DARK_GRAY;
		}
		String offset = offsets.get(handler);
		text = text.replaceAll("\n", "\n"+offset+" ");
		int headStart = offset.length()+heading.length();
		boolean setVisible = !factoryInstances.get(handler);
		window.printText(offset+heading+text+"\n", color, headStart,headStart+headEnd,setVisible);
	}
}
