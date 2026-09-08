/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.content.SharedPreferences
 *  android.os.Build
 *  android.util.Log
 *  androidx.compose.runtime.internal.StabilityInferred
 *  com.google.android.gms.tasks.Task
 *  com.google.firebase.firestore.DocumentChange
 *  com.google.firebase.firestore.DocumentChange$Type
 *  com.google.firebase.firestore.DocumentReference
 *  com.google.firebase.firestore.DocumentSnapshot
 *  com.google.firebase.firestore.FieldValue
 *  com.google.firebase.firestore.FirebaseFirestore
 *  com.google.firebase.firestore.FirebaseFirestoreException
 *  com.google.firebase.firestore.ListenerRegistration
 *  com.google.firebase.firestore.QueryDocumentSnapshot
 *  com.google.firebase.firestore.QuerySnapshot
 *  com.google.firebase.firestore.SetOptions
 *  kotlin.Lazy
 *  kotlin.LazyKt
 *  kotlin.Metadata
 *  kotlin.NoWhenBranchMatchedException
 *  kotlin.Pair
 *  kotlin.ResultKt
 *  kotlin.TuplesKt
 *  kotlin.Unit
 *  kotlin.collections.CollectionsKt
 *  kotlin.collections.MapsKt
 *  kotlin.coroutines.Continuation
 *  kotlin.coroutines.CoroutineContext
 *  kotlin.coroutines.intrinsics.IntrinsicsKt
 *  kotlin.coroutines.jvm.internal.Boxing
 *  kotlin.coroutines.jvm.internal.ContinuationImpl
 *  kotlin.coroutines.jvm.internal.SpillingKt
 *  kotlin.jvm.functions.Function2
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.Ref$BooleanRef
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.text.StringsKt
 *  kotlinx.coroutines.BuildersKt
 *  kotlinx.coroutines.CoroutineScope
 *  kotlinx.coroutines.Dispatchers
 *  kotlinx.coroutines.flow.FlowKt
 *  kotlinx.coroutines.flow.MutableSharedFlow
 *  kotlinx.coroutines.flow.MutableStateFlow
 *  kotlinx.coroutines.flow.SharedFlow
 *  kotlinx.coroutines.flow.SharedFlowKt
 *  kotlinx.coroutines.flow.StateFlow
 *  kotlinx.coroutines.flow.StateFlowKt
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.example.data.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.compose.runtime.internal.StabilityInferred;
import com.example.data.local.KapterkaDao;
import com.example.data.model.InventoryItem;
import com.example.data.model.OperationRecord;
import com.example.data.model.OperationType;
import com.example.data.model.RequestStatus;
import com.example.data.model.RequisitionRequest;
import com.example.data.model.StockRecord;
import com.example.data.model.WarehousePoint;
import com.example.data.sync.FirebaseSyncManager;
import com.example.data.sync.SyncState;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.Pair;
import kotlin.ResultKt;
import kotlin.TuplesKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.ContinuationImpl;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableSharedFlow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.SharedFlow;
import kotlinx.coroutines.flow.SharedFlowKt;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\u0088\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0004\b\b\u0010\tJ\u001e\u0010'\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u0010*\u001a\u00020\u000b2\u0006\u0010+\u001a\u00020\u000bJ\u0010\u0010,\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000bH\u0002J \u0010-\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u0010*\u001a\u00020\u000b2\u0006\u0010+\u001a\u00020\u000bH\u0002J\u0016\u0010.\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000bH\u0086@\u00a2\u0006\u0002\u0010/J$\u00100\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u00101\u001a\u0002022\f\u00103\u001a\b\u0012\u0004\u0012\u00020504J\u0016\u00106\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u00107\u001a\u000208J\u0016\u00109\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u0010:\u001a\u00020\u000bJ\u0016\u0010;\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u0010<\u001a\u00020\u000bJ\u0016\u0010=\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u0010>\u001a\u00020?J\u0016\u0010@\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u0010A\u001a\u00020\u000bJ\u0016\u0010B\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u0010C\u001a\u00020DJ\u0016\u0010E\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000b2\u0006\u0010F\u001a\u00020\u000bJ\u0006\u0010G\u001a\u00020(J\u000e\u0010H\u001a\u00020(2\u0006\u0010)\u001a\u00020\u000bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082D\u00a2\u0006\u0002\n\u0000R\u001b\u0010\f\u001a\u00020\r8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0010\u0010\u0011\u001a\u0004\b\u000e\u0010\u000fR\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00140\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0014\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u000b0\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u000b0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u001b\u0010\u001f\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\"\u0010\u0011\u001a\u0004\b \u0010!R\u000e\u0010#\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010$\u001a\b\u0012\u0004\u0012\u00020&0%X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006I"}, d2={"Lcom/example/data/sync/FirebaseSyncManager;", "", "context", "Landroid/content/Context;", "dao", "Lcom/example/data/local/KapterkaDao;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "<init>", "(Landroid/content/Context;Lcom/example/data/local/KapterkaDao;Lkotlinx/coroutines/CoroutineScope;)V", "TAG", "", "firestore", "Lcom/google/firebase/firestore/FirebaseFirestore;", "getFirestore", "()Lcom/google/firebase/firestore/FirebaseFirestore;", "firestore$delegate", "Lkotlin/Lazy;", "_syncState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/data/sync/SyncState;", "syncState", "Lkotlinx/coroutines/flow/StateFlow;", "getSyncState", "()Lkotlinx/coroutines/flow/StateFlow;", "_syncEvents", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "syncEvents", "Lkotlinx/coroutines/flow/SharedFlow;", "getSyncEvents", "()Lkotlinx/coroutines/flow/SharedFlow;", "deviceId", "getDeviceId", "()Ljava/lang/String;", "deviceId$delegate", "activeUnitKey", "listeners", "", "Lcom/google/firebase/firestore/ListenerRegistration;", "startSyncForUnit", "", "unitKey", "callsign", "unitName", "registerUnitListeners", "sendPresencePing", "pushAllLocalData", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "pushOperationAsync", "op", "Lcom/example/data/model/OperationRecord;", "updatedStocks", "", "Lcom/example/data/model/StockRecord;", "pushRequisitionAsync", "r", "Lcom/example/data/model/RequisitionRequest;", "deleteRequisitionAsync", "reqId", "deleteOperationAsync", "opId", "pushWarehousePointAsync", "p", "Lcom/example/data/model/WarehousePoint;", "deleteWarehousePointAsync", "pointId", "pushInventoryItemAsync", "item", "Lcom/example/data/model/InventoryItem;", "deleteInventoryItemAsync", "itemId", "stopSync", "clearCloudDataAsync", "app"})
@StabilityInferred(parameters=0)
@SourceDebugExtension(value={"SMAP\nFirebaseSyncManager.kt\nKotlin\n*S Kotlin\n*F\n+ 1 FirebaseSyncManager.kt\ncom/example/data/sync/FirebaseSyncManager\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,673:1\n1788#2,4:674\n*S KotlinDebug\n*F\n+ 1 FirebaseSyncManager.kt\ncom/example/data/sync/FirebaseSyncManager\n*L\n333#1:674,4\n*E\n"})
public final class FirebaseSyncManager {
    @NotNull
    private final Context context;
    @NotNull
    private final KapterkaDao dao;
    @NotNull
    private final CoroutineScope scope;
    @NotNull
    private final String TAG;
    @NotNull
    private final Lazy firestore$delegate;
    @NotNull
    private final MutableStateFlow<SyncState> _syncState;
    @NotNull
    private final StateFlow<SyncState> syncState;
    @NotNull
    private final MutableSharedFlow<String> _syncEvents;
    @NotNull
    private final SharedFlow<String> syncEvents;
    @NotNull
    private final Lazy deviceId$delegate;
    @NotNull
    private String activeUnitKey;
    @NotNull
    private List<ListenerRegistration> listeners;
    public static final int $stable = 8;

    public FirebaseSyncManager(@NotNull Context context, @NotNull KapterkaDao dao, @NotNull CoroutineScope scope) {
        Intrinsics.checkNotNullParameter((Object)context, (String)"context");
        Intrinsics.checkNotNullParameter((Object)dao, (String)"dao");
        Intrinsics.checkNotNullParameter((Object)scope, (String)"scope");
        this.context = context;
        this.dao = dao;
        this.scope = scope;
        this.TAG = "KapterkaSync";
        this.firestore$delegate = LazyKt.lazy(FirebaseSyncManager::firestore_delegate$lambda$0);
        this._syncState = StateFlowKt.MutableStateFlow((Object)new SyncState(false, 0L, false, 0, null, 31, null));
        this.syncState = FlowKt.asStateFlow(this._syncState);
        this._syncEvents = SharedFlowKt.MutableSharedFlow$default((int)0, (int)0, null, (int)7, null);
        this.syncEvents = (SharedFlow)this._syncEvents;
        this.deviceId$delegate = LazyKt.lazy(() -> FirebaseSyncManager.deviceId_delegate$lambda$1(this));
        this.activeUnitKey = "";
        this.listeners = new ArrayList();
    }

    private final FirebaseFirestore getFirestore() {
        Lazy lazy = this.firestore$delegate;
        return (FirebaseFirestore)lazy.getValue();
    }

    @NotNull
    public final StateFlow<SyncState> getSyncState() {
        return this.syncState;
    }

    @NotNull
    public final SharedFlow<String> getSyncEvents() {
        return this.syncEvents;
    }

    private final String getDeviceId() {
        Lazy lazy = this.deviceId$delegate;
        return (String)lazy.getValue();
    }

    public final void startSyncForUnit(@NotNull String unitKey, @NotNull String callsign, @NotNull String unitName) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        Intrinsics.checkNotNullParameter((Object)callsign, (String)"callsign");
        Intrinsics.checkNotNullParameter((Object)unitName, (String)"unitName");
        String cleanKey = ((Object)StringsKt.trim((CharSequence)unitKey)).toString();
        if (((CharSequence)cleanKey).length() == 0) {
            return;
        }
        if (Intrinsics.areEqual((Object)this.activeUnitKey, (Object)cleanKey) && !((Collection)this.listeners).isEmpty()) {
            this.sendPresencePing(cleanKey, callsign, unitName);
            return;
        }
        this.stopSync();
        this.activeUnitKey = cleanKey;
        this._syncState.setValue((Object)SyncState.copy$default((SyncState)this._syncState.getValue(), true, 0L, false, 0, "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043a \u043a\u0430\u043d\u0430\u043b\u0443 \u043f\u043e\u0434\u0440\u0430\u0437\u0434\u0435\u043b\u0435\u043d\u0438\u044f [" + cleanKey + "]...", 14, null));
        try {
            this.registerUnitListeners(cleanKey);
            this.sendPresencePing(cleanKey, callsign, unitName);
            BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, cleanKey, null){
                Object L$0;
                int label;
                final /* synthetic */ FirebaseSyncManager this$0;
                final /* synthetic */ String $cleanKey;
                {
                    this.this$0 = $receiver;
                    this.$cleanKey = $cleanKey;
                    super(2, $completion);
                }

                /*
                 * Exception decompiling
                 */
                public final Object invokeSuspend(Object $result) {
                    /*
                     * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
                     * 
                     * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [6[CASE], 3[SWITCH]], but top level block is 2[TRYBLOCK]
                     *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
                     *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
                     *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
                     *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
                     *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
                     *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
                     *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
                     *     at org.benf.cfr.reader.entities.Method.dump(Method.java:598)
                     *     at org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperAnonymousInner.dumpWithArgs(ClassFileDumperAnonymousInner.java:87)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner.dumpInner(ConstructorInvokationAnonymousInner.java:82)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dumpWithOuterPrecedence(AbstractExpression.java:142)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression.dumpInner(CastExpression.java:114)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dumpWithOuterPrecedence(AbstractExpression.java:139)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression.dumpInner(CastExpression.java:114)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dumpWithOuterPrecedence(AbstractExpression.java:142)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dump(AbstractExpression.java:98)
                     *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation.dumpInner(StaticFunctionInvokation.java:143)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dumpWithOuterPrecedence(AbstractExpression.java:142)
                     *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dump(AbstractExpression.java:98)
                     *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                     *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement.dump(StructuredExpressionStatement.java:29)
                     *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                     *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.dump(Op04StructuredStatement.java:220)
                     *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.Block.dump(Block.java:564)
                     *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                     *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.dump(Op04StructuredStatement.java:220)
                     *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredTry.dump(StructuredTry.java:79)
                     *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                     *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.dump(Op04StructuredStatement.java:220)
                     *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.Block.dump(Block.java:564)
                     *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                     *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.dump(Op04StructuredStatement.java:220)
                     *     at org.benf.cfr.reader.entities.attributes.AttributeCode.dump(AttributeCode.java:135)
                     *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                     *     at org.benf.cfr.reader.entities.Method.dump(Method.java:627)
                     *     at org.benf.cfr.reader.entities.classfilehelpers.AbstractClassFileDumper.dumpMethods(AbstractClassFileDumper.java:211)
                     *     at org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperNormal.dump(ClassFileDumperNormal.java:70)
                     *     at org.benf.cfr.reader.entities.ClassFile.dump(ClassFile.java:1167)
                     *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:952)
                     *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
                     *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
                     *     at org.benf.cfr.reader.Main.main(Main.java:54)
                     */
                    throw new IllegalStateException("Decompilation failed");
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)2, null);
        }
        catch (Exception e) {
            Log.e((String)this.TAG, (String)"Error starting sync", (Throwable)e);
            SyncState syncState = (SyncState)this._syncState.getValue();
            String string = e.getLocalizedMessage();
            if (string == null) {
                string = "\u041d\u0435\u0442 \u0441\u0435\u0442\u0438";
            }
            this._syncState.setValue((Object)SyncState.copy$default(syncState, false, 0L, false, 0, "\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f: " + string, 10, null));
        }
    }

    private final void registerUnitListeners(String unitKey) {
        DocumentReference documentReference = this.getFirestore().collection("units").document(unitKey);
        Intrinsics.checkNotNullExpressionValue((Object)documentReference, (String)"document(...)");
        DocumentReference unitRef = documentReference;
        Ref.BooleanRef isFirstOpLoad = new Ref.BooleanRef();
        isFirstOpLoad.element = true;
        ListenerRegistration listenerRegistration = unitRef.collection("warehouse_points").addSnapshotListener((arg_0, arg_1) -> FirebaseSyncManager.registerUnitListeners$lambda$2(this, arg_0, arg_1));
        Intrinsics.checkNotNullExpressionValue((Object)listenerRegistration, (String)"addSnapshotListener(...)");
        ListenerRegistration pointsListener2 = listenerRegistration;
        this.listeners.add(pointsListener2);
        ListenerRegistration listenerRegistration2 = unitRef.collection("inventory_items").addSnapshotListener((arg_0, arg_1) -> FirebaseSyncManager.registerUnitListeners$lambda$3(this, arg_0, arg_1));
        Intrinsics.checkNotNullExpressionValue((Object)listenerRegistration2, (String)"addSnapshotListener(...)");
        ListenerRegistration itemsListener2 = listenerRegistration2;
        this.listeners.add(itemsListener2);
        ListenerRegistration listenerRegistration3 = unitRef.collection("stock_records").addSnapshotListener((arg_0, arg_1) -> FirebaseSyncManager.registerUnitListeners$lambda$4(this, arg_0, arg_1));
        Intrinsics.checkNotNullExpressionValue((Object)listenerRegistration3, (String)"addSnapshotListener(...)");
        ListenerRegistration stockListener2 = listenerRegistration3;
        this.listeners.add(stockListener2);
        ListenerRegistration listenerRegistration4 = unitRef.collection("operation_records").addSnapshotListener((arg_0, arg_1) -> FirebaseSyncManager.registerUnitListeners$lambda$5(this, isFirstOpLoad, arg_0, arg_1));
        Intrinsics.checkNotNullExpressionValue((Object)listenerRegistration4, (String)"addSnapshotListener(...)");
        ListenerRegistration opListener2 = listenerRegistration4;
        this.listeners.add(opListener2);
        ListenerRegistration listenerRegistration5 = unitRef.collection("requisitions").addSnapshotListener((arg_0, arg_1) -> FirebaseSyncManager.registerUnitListeners$lambda$6(this, arg_0, arg_1));
        Intrinsics.checkNotNullExpressionValue((Object)listenerRegistration5, (String)"addSnapshotListener(...)");
        ListenerRegistration reqListener2 = listenerRegistration5;
        this.listeners.add(reqListener2);
        ListenerRegistration listenerRegistration6 = unitRef.collection("devices").addSnapshotListener((arg_0, arg_1) -> FirebaseSyncManager.registerUnitListeners$lambda$8(this, arg_0, arg_1));
        Intrinsics.checkNotNullExpressionValue((Object)listenerRegistration6, (String)"addSnapshotListener(...)");
        ListenerRegistration presenceListener = listenerRegistration6;
        this.listeners.add(presenceListener);
    }

    private final void sendPresencePing(String unitKey, String callsign, String unitName) {
        DocumentReference documentReference = this.getFirestore().collection("units").document(unitKey);
        Intrinsics.checkNotNullExpressionValue((Object)documentReference, (String)"document(...)");
        DocumentReference unitRef = documentReference;
        Pair[] pairArray = new Pair[]{TuplesKt.to((Object)"deviceId", (Object)this.getDeviceId()), TuplesKt.to((Object)"callsign", (Object)callsign), TuplesKt.to((Object)"unitName", (Object)unitName), TuplesKt.to((Object)"deviceModel", (Object)(Build.MANUFACTURER + " " + Build.MODEL)), TuplesKt.to((Object)"timestamp", (Object)FieldValue.serverTimestamp()), TuplesKt.to((Object)"timestampMillis", (Object)System.currentTimeMillis())};
        HashMap data = MapsKt.hashMapOf((Pair[])pairArray);
        unitRef.collection("devices").document(this.getDeviceId()).set((Object)data, SetOptions.merge());
        pairArray = new Pair[]{TuplesKt.to((Object)"unitKey", (Object)unitKey), TuplesKt.to((Object)"unitName", (Object)unitName), TuplesKt.to((Object)"lastActivity", (Object)FieldValue.serverTimestamp())};
        unitRef.set((Object)MapsKt.hashMapOf((Pair[])pairArray), SetOptions.merge());
    }

    /*
     * Unable to fully structure code
     */
    @Nullable
    public final Object pushAllLocalData(@NotNull String unitKey, @NotNull Continuation<? super Unit> $completion) {
        if (!($completion instanceof pushAllLocalData.1)) ** GOTO lbl-1000
        var13_3 = $completion;
        if ((var13_3.label & -2147483648) != 0) {
            var13_3.label -= -2147483648;
        } else lbl-1000:
        // 2 sources

        {
            $continuation = new ContinuationImpl(this, $completion){
                Object L$0;
                Object L$1;
                Object L$2;
                Object L$3;
                Object L$4;
                Object L$5;
                /* synthetic */ Object result;
                final /* synthetic */ FirebaseSyncManager this$0;
                int label;
                {
                    this.this$0 = this$0;
                    super($completion);
                }

                @Nullable
                public final Object invokeSuspend(@NotNull Object $result) {
                    this.result = $result;
                    this.label |= Integer.MIN_VALUE;
                    return this.this$0.pushAllLocalData(null, (Continuation<? super Unit>)((Continuation)this));
                }
            };
        }
        $result = $continuation.result;
        var14_5 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch ($continuation.label) {
            case 0: {
                ResultKt.throwOnFailure((Object)$result);
                if (((CharSequence)unitKey).length() == 0) {
                    return Unit.INSTANCE;
                }
                this._syncState.setValue((Object)SyncState.copy$default((SyncState)this._syncState.getValue(), true, 0L, false, 0, "\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430 \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0445 \u0434\u0430\u043d\u043d\u044b\u0445 \u0432 \u043e\u0431\u043b\u0430\u043a\u043e...", 14, null));
                v0 = this.getFirestore().collection("units").document(unitKey);
                Intrinsics.checkNotNullExpressionValue((Object)v0, (String)"document(...)");
                unitRef = v0;
                $continuation.L$0 = SpillingKt.nullOutSpilledVariable((Object)unitKey);
                $continuation.L$1 = unitRef;
                $continuation.label = 1;
                v1 = FlowKt.first(this.dao.getAllPoints(), (Continuation)$continuation);
                ** if (v1 != var14_5) goto lbl26
lbl25:
                // 1 sources

                return var14_5;
lbl26:
                // 1 sources

                ** GOTO lbl34
            }
            case 1: {
                unitRef = (DocumentReference)$continuation.L$1;
                unitKey = (String)$continuation.L$0;
                ResultKt.throwOnFailure((Object)$result);
                v1 = $result;
lbl34:
                // 2 sources

                points = (List)v1;
                for (Object p : points) {
                    var7_11 = new Pair[]{TuplesKt.to((Object)"id", (Object)p.getId()), TuplesKt.to((Object)"name", (Object)p.getName()), TuplesKt.to((Object)"description", (Object)p.getDescription()), TuplesKt.to((Object)"isBase", (Object)Boxing.boxBoolean((boolean)p.isBase())), TuplesKt.to((Object)"orderIndex", (Object)Boxing.boxInt((int)p.getOrderIndex())), TuplesKt.to((Object)"createdAt", (Object)Boxing.boxLong((long)p.getCreatedAt()))};
                    unitRef.collection("warehouse_points").document(p.getId()).set((Object)MapsKt.hashMapOf((Pair[])var7_11), SetOptions.merge());
                }
                $continuation.L$0 = SpillingKt.nullOutSpilledVariable((Object)unitKey);
                $continuation.L$1 = unitRef;
                $continuation.L$2 = SpillingKt.nullOutSpilledVariable((Object)points);
                $continuation.label = 2;
                v2 = FlowKt.first(this.dao.getAllItems(), (Continuation)$continuation);
                ** if (v2 != var14_5) goto lbl47
lbl46:
                // 1 sources

                return var14_5;
lbl47:
                // 1 sources

                ** GOTO lbl56
            }
            case 2: {
                points = (List)$continuation.L$2;
                unitRef = (DocumentReference)$continuation.L$1;
                unitKey = (String)$continuation.L$0;
                ResultKt.throwOnFailure((Object)$result);
                v2 = $result;
lbl56:
                // 2 sources

                items = (List)v2;
                for (Object item : items) {
                    var8_12 = new Pair[]{TuplesKt.to((Object)"id", (Object)item.getId()), TuplesKt.to((Object)"name", (Object)item.getName()), TuplesKt.to((Object)"serviceCategory", (Object)item.getServiceCategory()), TuplesKt.to((Object)"subType", (Object)item.getSubType()), TuplesKt.to((Object)"unit", (Object)item.getUnit()), TuplesKt.to((Object)"categoryClass", (Object)item.getCategoryClass()), TuplesKt.to((Object)"standardCode", (Object)item.getStandardCode()), TuplesKt.to((Object)"isCustom", (Object)Boxing.boxBoolean((boolean)item.isCustom()))};
                    unitRef.collection("inventory_items").document(item.getId()).set((Object)MapsKt.hashMapOf((Pair[])var8_12), SetOptions.merge());
                }
                $continuation.L$0 = SpillingKt.nullOutSpilledVariable((Object)unitKey);
                $continuation.L$1 = unitRef;
                $continuation.L$2 = SpillingKt.nullOutSpilledVariable((Object)points);
                $continuation.L$3 = SpillingKt.nullOutSpilledVariable((Object)items);
                $continuation.label = 3;
                v3 = FlowKt.first(this.dao.getAllStockRecords(), (Continuation)$continuation);
                ** if (v3 != var14_5) goto lbl70
lbl69:
                // 1 sources

                return var14_5;
lbl70:
                // 1 sources

                ** GOTO lbl80
            }
            case 3: {
                items = (List)$continuation.L$3;
                points = (List)$continuation.L$2;
                unitRef = (DocumentReference)$continuation.L$1;
                unitKey = (String)$continuation.L$0;
                ResultKt.throwOnFailure((Object)$result);
                v3 = $result;
lbl80:
                // 2 sources

                stocks = (List)v3;
                for (Object s : stocks) {
                    docId = s.getPointId() + "___" + s.getItemId();
                    var10_14 = new Pair[]{TuplesKt.to((Object)"pointId", (Object)s.getPointId()), TuplesKt.to((Object)"itemId", (Object)s.getItemId()), TuplesKt.to((Object)"quantity", (Object)Boxing.boxInt((int)s.getQuantity())), TuplesKt.to((Object)"incomeTotal", (Object)Boxing.boxInt((int)s.getIncomeTotal())), TuplesKt.to((Object)"expenseTotal", (Object)Boxing.boxInt((int)s.getExpenseTotal())), TuplesKt.to((Object)"lastUpdated", (Object)Boxing.boxLong((long)s.getLastUpdated()))};
                    unitRef.collection("stock_records").document(docId).set((Object)MapsKt.hashMapOf((Pair[])var10_14), SetOptions.merge());
                }
                $continuation.L$0 = SpillingKt.nullOutSpilledVariable((Object)unitKey);
                $continuation.L$1 = unitRef;
                $continuation.L$2 = SpillingKt.nullOutSpilledVariable((Object)points);
                $continuation.L$3 = SpillingKt.nullOutSpilledVariable((Object)items);
                $continuation.L$4 = SpillingKt.nullOutSpilledVariable((Object)stocks);
                $continuation.label = 4;
                v4 = FlowKt.first(this.dao.getAllOperations(), (Continuation)$continuation);
                ** if (v4 != var14_5) goto lbl96
lbl95:
                // 1 sources

                return var14_5;
lbl96:
                // 1 sources

                ** GOTO lbl107
            }
            case 4: {
                stocks = (List)$continuation.L$4;
                items = (List)$continuation.L$3;
                points = (List)$continuation.L$2;
                unitRef = (DocumentReference)$continuation.L$1;
                unitKey = (String)$continuation.L$0;
                ResultKt.throwOnFailure((Object)$result);
                v4 = $result;
lbl107:
                // 2 sources

                ops = (List)v4;
                for (OperationRecord op : ops) {
                    var10_14 = new Pair[]{TuplesKt.to((Object)"id", (Object)op.getId()), TuplesKt.to((Object)"type", (Object)op.getType().name()), TuplesKt.to((Object)"fromPointName", (Object)op.getFromPointName()), TuplesKt.to((Object)"toPointName", (Object)op.getToPointName()), TuplesKt.to((Object)"docNumber", (Object)op.getDocNumber()), TuplesKt.to((Object)"responsiblePerson", (Object)op.getResponsiblePerson()), TuplesKt.to((Object)"comment", (Object)op.getComment()), TuplesKt.to((Object)"timestamp", (Object)Boxing.boxLong((long)op.getTimestamp())), TuplesKt.to((Object)"itemsSummary", (Object)op.getItemsSummary()), TuplesKt.to((Object)"itemsJson", (Object)op.getItemsJson())};
                    unitRef.collection("operation_records").document(op.getId()).set((Object)MapsKt.hashMapOf((Pair[])var10_14), SetOptions.merge());
                }
                $continuation.L$0 = SpillingKt.nullOutSpilledVariable((Object)unitKey);
                $continuation.L$1 = unitRef;
                $continuation.L$2 = SpillingKt.nullOutSpilledVariable((Object)points);
                $continuation.L$3 = SpillingKt.nullOutSpilledVariable((Object)items);
                $continuation.L$4 = SpillingKt.nullOutSpilledVariable((Object)stocks);
                $continuation.L$5 = SpillingKt.nullOutSpilledVariable((Object)ops);
                $continuation.label = 5;
                v5 = FlowKt.first(this.dao.getAllRequisitions(), (Continuation)$continuation);
                ** if (v5 != var14_5) goto lbl123
lbl122:
                // 1 sources

                return var14_5;
lbl123:
                // 1 sources

                ** GOTO lbl135
            }
            case 5: {
                ops = (List)$continuation.L$5;
                stocks = (List)$continuation.L$4;
                items = (List)$continuation.L$3;
                points = (List)$continuation.L$2;
                unitRef = (DocumentReference)$continuation.L$1;
                unitKey = (String)$continuation.L$0;
                try {
                    ResultKt.throwOnFailure((Object)$result);
                    v5 = $result;
lbl135:
                    // 2 sources

                    reqs = (List)v5;
                    for (RequisitionRequest r : reqs) {
                        var11_16 = new Pair[]{TuplesKt.to((Object)"id", (Object)r.getId()), TuplesKt.to((Object)"pointName", (Object)r.getPointName()), TuplesKt.to((Object)"applicantName", (Object)r.getApplicantName()), TuplesKt.to((Object)"status", (Object)r.getStatus().name()), TuplesKt.to((Object)"comment", (Object)r.getComment()), TuplesKt.to((Object)"timestamp", (Object)Boxing.boxLong((long)r.getTimestamp())), TuplesKt.to((Object)"itemsSummary", (Object)r.getItemsSummary()), TuplesKt.to((Object)"itemsJson", (Object)r.getItemsJson())};
                        unitRef.collection("requisitions").document(r.getId()).set((Object)MapsKt.hashMapOf((Pair[])var11_16), SetOptions.merge());
                    }
                    var9_13 = (SyncState)this._syncState.getValue();
                    var10_15 = System.currentTimeMillis();
                    this._syncState.setValue((Object)SyncState.copy$default(var9_13, false, var10_15, true, 0, "\u0411\u0430\u0437\u0430 \u043f\u043e\u0434\u0440\u0430\u0437\u0434\u0435\u043b\u0435\u043d\u0438\u044f \u0441\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u043d\u0430 (\u043e\u043d\u043b\u0430\u0439\u043d)", 8, null));
                }
                catch (Exception e) {
                    Log.e((String)this.TAG, (String)"Error pushing local data", (Throwable)e);
                    this._syncState.setValue((Object)SyncState.copy$default((SyncState)this._syncState.getValue(), false, 0L, false, 0, "\u0414\u0430\u043d\u043d\u044b\u0435 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u044b \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u043e (\u043e\u0436\u0438\u0434\u0430\u043d\u0438\u0435 \u0441\u0435\u0442\u0438)", 14, null));
                }
                return Unit.INSTANCE;
            }
        }
        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
    }

    public final void pushOperationAsync(@NotNull String unitKey, @NotNull OperationRecord op, @NotNull List<StockRecord> updatedStocks) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        Intrinsics.checkNotNullParameter((Object)op, (String)"op");
        Intrinsics.checkNotNullParameter(updatedStocks, (String)"updatedStocks");
        if (((CharSequence)unitKey).length() == 0) {
            return;
        }
        BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, unitKey, op, updatedStocks, null){
            int label;
            final /* synthetic */ FirebaseSyncManager this$0;
            final /* synthetic */ String $unitKey;
            final /* synthetic */ OperationRecord $op;
            final /* synthetic */ List<StockRecord> $updatedStocks;
            {
                this.this$0 = $receiver;
                this.$unitKey = $unitKey;
                this.$op = $op;
                this.$updatedStocks = $updatedStocks;
                super(2, $completion);
            }

            public final Object invokeSuspend(Object $result) {
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        DocumentReference documentReference = FirebaseSyncManager.access$getFirestore(this.this$0).collection("units").document(this.$unitKey);
                        Intrinsics.checkNotNullExpressionValue((Object)documentReference, (String)"document(...)");
                        DocumentReference unitRef = documentReference;
                        try {
                            Pair[] pairArray = new Pair[]{TuplesKt.to((Object)"id", (Object)this.$op.getId()), TuplesKt.to((Object)"type", (Object)this.$op.getType().name()), TuplesKt.to((Object)"fromPointName", (Object)this.$op.getFromPointName()), TuplesKt.to((Object)"toPointName", (Object)this.$op.getToPointName()), TuplesKt.to((Object)"docNumber", (Object)this.$op.getDocNumber()), TuplesKt.to((Object)"responsiblePerson", (Object)this.$op.getResponsiblePerson()), TuplesKt.to((Object)"comment", (Object)this.$op.getComment()), TuplesKt.to((Object)"timestamp", (Object)Boxing.boxLong((long)this.$op.getTimestamp())), TuplesKt.to((Object)"itemsSummary", (Object)this.$op.getItemsSummary()), TuplesKt.to((Object)"itemsJson", (Object)this.$op.getItemsJson())};
                            unitRef.collection("operation_records").document(this.$op.getId()).set((Object)MapsKt.hashMapOf((Pair[])pairArray), SetOptions.merge());
                            for (StockRecord s : this.$updatedStocks) {
                                String docId = s.getPointId() + "___" + s.getItemId();
                                Pair[] pairArray2 = new Pair[]{TuplesKt.to((Object)"pointId", (Object)s.getPointId()), TuplesKt.to((Object)"itemId", (Object)s.getItemId()), TuplesKt.to((Object)"quantity", (Object)Boxing.boxInt((int)s.getQuantity())), TuplesKt.to((Object)"incomeTotal", (Object)Boxing.boxInt((int)s.getIncomeTotal())), TuplesKt.to((Object)"expenseTotal", (Object)Boxing.boxInt((int)s.getExpenseTotal())), TuplesKt.to((Object)"lastUpdated", (Object)Boxing.boxLong((long)s.getLastUpdated()))};
                                unitRef.collection("stock_records").document(docId).set((Object)MapsKt.hashMapOf((Pair[])pairArray2), SetOptions.merge());
                            }
                            FirebaseSyncManager.access$get_syncState$p(this.this$0).setValue((Object)SyncState.copy$default((SyncState)FirebaseSyncManager.access$get_syncState$p(this.this$0).getValue(), false, System.currentTimeMillis(), true, 0, "\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u044f \u0441\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u043d\u0430", 9, null));
                        }
                        catch (Exception e) {
                            Log.w((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Failed pushing op live, saved in Firestore cache", (Throwable)e);
                        }
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    public final void pushRequisitionAsync(@NotNull String unitKey, @NotNull RequisitionRequest r) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        Intrinsics.checkNotNullParameter((Object)r, (String)"r");
        if (((CharSequence)unitKey).length() == 0) {
            return;
        }
        BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, unitKey, r, null){
            int label;
            final /* synthetic */ FirebaseSyncManager this$0;
            final /* synthetic */ String $unitKey;
            final /* synthetic */ RequisitionRequest $r;
            {
                this.this$0 = $receiver;
                this.$unitKey = $unitKey;
                this.$r = $r;
                super(2, $completion);
            }

            public final Object invokeSuspend(Object $result) {
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        try {
                            Pair[] pairArray = new Pair[]{TuplesKt.to((Object)"id", (Object)this.$r.getId()), TuplesKt.to((Object)"pointName", (Object)this.$r.getPointName()), TuplesKt.to((Object)"applicantName", (Object)this.$r.getApplicantName()), TuplesKt.to((Object)"status", (Object)this.$r.getStatus().name()), TuplesKt.to((Object)"comment", (Object)this.$r.getComment()), TuplesKt.to((Object)"timestamp", (Object)Boxing.boxLong((long)this.$r.getTimestamp())), TuplesKt.to((Object)"itemsSummary", (Object)this.$r.getItemsSummary()), TuplesKt.to((Object)"itemsJson", (Object)this.$r.getItemsJson())};
                            Task task = FirebaseSyncManager.access$getFirestore(this.this$0).collection("units").document(this.$unitKey).collection("requisitions").document(this.$r.getId()).set((Object)MapsKt.hashMapOf((Pair[])pairArray), SetOptions.merge());
                            Intrinsics.checkNotNull((Object)task);
                        }
                        catch (Exception e) {
                            Log.w((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Failed pushing req live", (Throwable)e);
                        }
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    public final void deleteRequisitionAsync(@NotNull String unitKey, @NotNull String reqId) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        Intrinsics.checkNotNullParameter((Object)reqId, (String)"reqId");
        if (((CharSequence)unitKey).length() == 0) {
            return;
        }
        BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, unitKey, reqId, null){
            int label;
            final /* synthetic */ FirebaseSyncManager this$0;
            final /* synthetic */ String $unitKey;
            final /* synthetic */ String $reqId;
            {
                this.this$0 = $receiver;
                this.$unitKey = $unitKey;
                this.$reqId = $reqId;
                super(2, $completion);
            }

            public final Object invokeSuspend(Object $result) {
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        try {
                            Task task = FirebaseSyncManager.access$getFirestore(this.this$0).collection("units").document(this.$unitKey).collection("requisitions").document(this.$reqId).delete();
                            Intrinsics.checkNotNull((Object)task);
                        }
                        catch (Exception e) {
                            Log.w((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Failed deleting req live", (Throwable)e);
                        }
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    public final void deleteOperationAsync(@NotNull String unitKey, @NotNull String opId) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        Intrinsics.checkNotNullParameter((Object)opId, (String)"opId");
        if (((CharSequence)unitKey).length() == 0) {
            return;
        }
        BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, unitKey, opId, null){
            int label;
            final /* synthetic */ FirebaseSyncManager this$0;
            final /* synthetic */ String $unitKey;
            final /* synthetic */ String $opId;
            {
                this.this$0 = $receiver;
                this.$unitKey = $unitKey;
                this.$opId = $opId;
                super(2, $completion);
            }

            public final Object invokeSuspend(Object $result) {
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        try {
                            Task task = FirebaseSyncManager.access$getFirestore(this.this$0).collection("units").document(this.$unitKey).collection("operation_records").document(this.$opId).delete();
                            Intrinsics.checkNotNull((Object)task);
                        }
                        catch (Exception e) {
                            Log.w((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Failed deleting operation live", (Throwable)e);
                        }
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    public final void pushWarehousePointAsync(@NotNull String unitKey, @NotNull WarehousePoint p) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        Intrinsics.checkNotNullParameter((Object)p, (String)"p");
        if (((CharSequence)unitKey).length() == 0) {
            return;
        }
        BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, unitKey, p, null){
            int label;
            final /* synthetic */ FirebaseSyncManager this$0;
            final /* synthetic */ String $unitKey;
            final /* synthetic */ WarehousePoint $p;
            {
                this.this$0 = $receiver;
                this.$unitKey = $unitKey;
                this.$p = $p;
                super(2, $completion);
            }

            public final Object invokeSuspend(Object $result) {
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        try {
                            Pair[] pairArray = new Pair[]{TuplesKt.to((Object)"id", (Object)this.$p.getId()), TuplesKt.to((Object)"name", (Object)this.$p.getName()), TuplesKt.to((Object)"description", (Object)this.$p.getDescription()), TuplesKt.to((Object)"isBase", (Object)Boxing.boxBoolean((boolean)this.$p.isBase())), TuplesKt.to((Object)"orderIndex", (Object)Boxing.boxInt((int)this.$p.getOrderIndex())), TuplesKt.to((Object)"createdAt", (Object)Boxing.boxLong((long)this.$p.getCreatedAt()))};
                            Task task = FirebaseSyncManager.access$getFirestore(this.this$0).collection("units").document(this.$unitKey).collection("warehouse_points").document(this.$p.getId()).set((Object)MapsKt.hashMapOf((Pair[])pairArray), SetOptions.merge());
                            Intrinsics.checkNotNull((Object)task);
                        }
                        catch (Exception e) {
                            Log.w((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Failed pushing point live", (Throwable)e);
                        }
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    public final void deleteWarehousePointAsync(@NotNull String unitKey, @NotNull String pointId) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        Intrinsics.checkNotNullParameter((Object)pointId, (String)"pointId");
        if (((CharSequence)unitKey).length() == 0) {
            return;
        }
        BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, unitKey, pointId, null){
            int label;
            final /* synthetic */ FirebaseSyncManager this$0;
            final /* synthetic */ String $unitKey;
            final /* synthetic */ String $pointId;
            {
                this.this$0 = $receiver;
                this.$unitKey = $unitKey;
                this.$pointId = $pointId;
                super(2, $completion);
            }

            public final Object invokeSuspend(Object $result) {
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        try {
                            Task task = FirebaseSyncManager.access$getFirestore(this.this$0).collection("units").document(this.$unitKey).collection("warehouse_points").document(this.$pointId).delete();
                            Intrinsics.checkNotNull((Object)task);
                        }
                        catch (Exception e) {
                            Log.w((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Failed deleting point live", (Throwable)e);
                        }
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    public final void pushInventoryItemAsync(@NotNull String unitKey, @NotNull InventoryItem item) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        Intrinsics.checkNotNullParameter((Object)item, (String)"item");
        if (((CharSequence)unitKey).length() == 0) {
            return;
        }
        BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, unitKey, item, null){
            int label;
            final /* synthetic */ FirebaseSyncManager this$0;
            final /* synthetic */ String $unitKey;
            final /* synthetic */ InventoryItem $item;
            {
                this.this$0 = $receiver;
                this.$unitKey = $unitKey;
                this.$item = $item;
                super(2, $completion);
            }

            public final Object invokeSuspend(Object $result) {
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        try {
                            Pair[] pairArray = new Pair[]{TuplesKt.to((Object)"id", (Object)this.$item.getId()), TuplesKt.to((Object)"name", (Object)this.$item.getName()), TuplesKt.to((Object)"serviceCategory", (Object)this.$item.getServiceCategory()), TuplesKt.to((Object)"subType", (Object)this.$item.getSubType()), TuplesKt.to((Object)"unit", (Object)this.$item.getUnit()), TuplesKt.to((Object)"categoryClass", (Object)this.$item.getCategoryClass()), TuplesKt.to((Object)"standardCode", (Object)this.$item.getStandardCode()), TuplesKt.to((Object)"isCustom", (Object)Boxing.boxBoolean((boolean)this.$item.isCustom()))};
                            Task task = FirebaseSyncManager.access$getFirestore(this.this$0).collection("units").document(this.$unitKey).collection("inventory_items").document(this.$item.getId()).set((Object)MapsKt.hashMapOf((Pair[])pairArray), SetOptions.merge());
                            Intrinsics.checkNotNull((Object)task);
                        }
                        catch (Exception e) {
                            Log.w((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Failed pushing item live", (Throwable)e);
                        }
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    public final void deleteInventoryItemAsync(@NotNull String unitKey, @NotNull String itemId) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        Intrinsics.checkNotNullParameter((Object)itemId, (String)"itemId");
        if (((CharSequence)unitKey).length() == 0) {
            return;
        }
        BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, unitKey, itemId, null){
            int label;
            final /* synthetic */ FirebaseSyncManager this$0;
            final /* synthetic */ String $unitKey;
            final /* synthetic */ String $itemId;
            {
                this.this$0 = $receiver;
                this.$unitKey = $unitKey;
                this.$itemId = $itemId;
                super(2, $completion);
            }

            public final Object invokeSuspend(Object $result) {
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        try {
                            Task task = FirebaseSyncManager.access$getFirestore(this.this$0).collection("units").document(this.$unitKey).collection("inventory_items").document(this.$itemId).delete();
                            Intrinsics.checkNotNull((Object)task);
                        }
                        catch (Exception e) {
                            Log.w((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Failed deleting item live", (Throwable)e);
                        }
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    public final void stopSync() {
        for (ListenerRegistration l : this.listeners) {
            try {
                l.remove();
            }
            catch (Exception exception) {}
        }
        this.listeners.clear();
    }

    public final void clearCloudDataAsync(@NotNull String unitKey) {
        Intrinsics.checkNotNullParameter((Object)unitKey, (String)"unitKey");
        BuildersKt.launch$default((CoroutineScope)this.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(unitKey, this, null){
            Object L$0;
            Object L$1;
            Object L$2;
            Object L$3;
            Object L$4;
            Object L$5;
            Object L$6;
            Object L$7;
            int label;
            final /* synthetic */ String $unitKey;
            final /* synthetic */ FirebaseSyncManager this$0;
            {
                this.$unitKey = $unitKey;
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Exception decompiling
             */
            public final Object invokeSuspend(Object $result) {
                /*
                 * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
                 * 
                 * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [0[TRYBLOCK]], but top level block is 9[WHILELOOP]
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
                 *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
                 *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
                 *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
                 *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
                 *     at org.benf.cfr.reader.entities.Method.dump(Method.java:598)
                 *     at org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperAnonymousInner.dumpWithArgs(ClassFileDumperAnonymousInner.java:87)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.ConstructorInvokationAnonymousInner.dumpInner(ConstructorInvokationAnonymousInner.java:82)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dumpWithOuterPrecedence(AbstractExpression.java:142)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression.dumpInner(CastExpression.java:114)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dumpWithOuterPrecedence(AbstractExpression.java:139)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression.dumpInner(CastExpression.java:114)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dumpWithOuterPrecedence(AbstractExpression.java:142)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dump(AbstractExpression.java:98)
                 *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation.dumpInner(StaticFunctionInvokation.java:143)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dumpWithOuterPrecedence(AbstractExpression.java:142)
                 *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractExpression.dump(AbstractExpression.java:98)
                 *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                 *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement.dump(StructuredExpressionStatement.java:29)
                 *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.dump(Op04StructuredStatement.java:220)
                 *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.Block.dump(Block.java:564)
                 *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                 *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.dump(Op04StructuredStatement.java:220)
                 *     at org.benf.cfr.reader.entities.attributes.AttributeCode.dump(AttributeCode.java:135)
                 *     at org.benf.cfr.reader.state.TypeUsageCollectingDumper.dump(TypeUsageCollectingDumper.java:194)
                 *     at org.benf.cfr.reader.entities.Method.dump(Method.java:627)
                 *     at org.benf.cfr.reader.entities.classfilehelpers.AbstractClassFileDumper.dumpMethods(AbstractClassFileDumper.java:211)
                 *     at org.benf.cfr.reader.entities.classfilehelpers.ClassFileDumperNormal.dump(ClassFileDumperNormal.java:70)
                 *     at org.benf.cfr.reader.entities.ClassFile.dump(ClassFile.java:1167)
                 *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:952)
                 *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
                 *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
                 *     at org.benf.cfr.reader.Main.main(Main.java:54)
                 */
                throw new IllegalStateException("Decompilation failed");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
    }

    private static final FirebaseFirestore firestore_delegate$lambda$0() {
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        Intrinsics.checkNotNullExpressionValue((Object)firebaseFirestore, (String)"getInstance(...)");
        return firebaseFirestore;
    }

    private static final String deviceId_delegate$lambda$1(FirebaseSyncManager this$0) {
        SharedPreferences prefs = this$0.context.getSharedPreferences("kapterka_sync_prefs", 0);
        Object id = prefs.getString("device_uuid", null);
        if (id == null) {
            String string = UUID.randomUUID().toString();
            Intrinsics.checkNotNullExpressionValue((Object)string, (String)"toString(...)");
            id = "dev_" + StringsKt.take((String)string, (int)8);
            prefs.edit().putString("device_uuid", (String)id).apply();
        }
        return id;
    }

    private static final void registerUnitListeners$lambda$2(FirebaseSyncManager this$0, QuerySnapshot snapshot, FirebaseFirestoreException error) {
        if (error != null) {
            Log.w((String)this$0.TAG, (String)"Points listener error", (Throwable)((Throwable)error));
            return;
        }
        if (snapshot != null) {
            BuildersKt.launch$default((CoroutineScope)this$0.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(snapshot, this$0, null){
                Object L$0;
                Object L$1;
                Object L$2;
                Object L$3;
                Object L$4;
                Object L$5;
                int I$0;
                int I$1;
                long J$0;
                int label;
                final /* synthetic */ QuerySnapshot $snapshot;
                final /* synthetic */ FirebaseSyncManager this$0;
                {
                    this.$snapshot = $snapshot;
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Recovered potentially malformed switches.  Disable with '--allowmalformedswitch false'
                 * Unable to fully structure code
                 * Enabled aggressive block sorting
                 */
                public final Object invokeSuspend(Object $result) {
                    var12_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    block0 : switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            var2_3 = this.$snapshot.getDocumentChanges().iterator();
                            break;
                        }
                        case 1: {
                            createdAt = this.J$0;
                            orderIndex = this.I$1;
                            isBase = this.I$0;
                            description = (String)this.L$5;
                            name = (String)this.L$4;
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v0 = $result;
                            break;
                        }
                        case 2: {
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            while (true) {
                                this.L$0 = var2_3;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)id);
                                this.label = 3;
                                v2 = FirebaseSyncManager.access$getDao$p(this.this$0).deleteStockForPoint(id, (Continuation<? super Unit>)((Continuation)this));
                                if (v2 != var12_2) break block0;
                                return var12_2;
                            }
                        }
                        case 3: {
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v2 = $result;
                            break;
                        }
                        default: {
                            throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                        }
                    }
                    block11: while (var2_3.hasNext()) {
                        change = (DocumentChange)var2_3.next();
                        Intrinsics.checkNotNullExpressionValue((Object)change.getDocument(), (String)"getDocument(...)");
                        v3 = doc.getString("id");
                        if (v3 == null) {
                            v4 = doc.getId();
                            v3 = v4;
                            Intrinsics.checkNotNullExpressionValue((Object)v4, (String)"getId(...)");
                        }
                        id = v3;
                        switch (registerUnitListeners.pointsListener.1.WhenMappings.$EnumSwitchMapping$0[change.getType().ordinal()]) {
                            case 1: 
                            case 2: {
                                if (doc.getString("name") == null) continue block11;
                                v5 = doc.getString("description");
                                if (v5 == null) {
                                    v5 = "";
                                }
                                description = v5;
                                v6 = doc.getBoolean("isBase");
                                isBase = (int)(v6 != null ? v6 : false);
                                v7 = doc.getLong("orderIndex");
                                orderIndex = (int)(v7 != null ? v7 : 0L);
                                v8 = doc.getLong("createdAt");
                                createdAt = v8 != null ? v8 : System.currentTimeMillis();
                                this.L$0 = var2_3;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)id);
                                this.L$4 = SpillingKt.nullOutSpilledVariable((Object)name);
                                this.L$5 = SpillingKt.nullOutSpilledVariable((Object)description);
                                this.I$0 = isBase;
                                this.I$1 = orderIndex;
                                this.J$0 = createdAt;
                                this.label = 1;
                                v0 = FirebaseSyncManager.access$getDao$p(this.this$0).insertPoint(new WarehousePoint(id, name, description, isBase != 0, orderIndex, createdAt), (Continuation<? super Unit>)((Continuation)this));
                                if (v0 != var12_2) continue block11;
                                return var12_2;
                            }
                            case 3: {
                                this.L$0 = var2_3;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = id;
                                this.L$4 = null;
                                this.L$5 = null;
                                this.label = 2;
                                if ((v1 = FirebaseSyncManager.access$getDao$p(this.this$0).deletePoint(id, (Continuation<? super Unit>)((Continuation)this))) != var12_2) ** continue;
                                return var12_2;
                            }
                        }
                        throw new NoWhenBranchMatchedException();
                    }
                    return Unit.INSTANCE;
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)2, null);
        }
    }

    private static final void registerUnitListeners$lambda$3(FirebaseSyncManager this$0, QuerySnapshot snapshot, FirebaseFirestoreException error) {
        if (error != null) {
            Log.w((String)this$0.TAG, (String)"Items listener error", (Throwable)((Throwable)error));
            return;
        }
        if (snapshot != null) {
            BuildersKt.launch$default((CoroutineScope)this$0.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(snapshot, this$0, null){
                Object L$0;
                Object L$1;
                Object L$2;
                Object L$3;
                Object L$4;
                Object L$5;
                Object L$6;
                Object L$7;
                Object L$8;
                Object L$9;
                int I$0;
                int label;
                final /* synthetic */ QuerySnapshot $snapshot;
                final /* synthetic */ FirebaseSyncManager this$0;
                {
                    this.$snapshot = $snapshot;
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Recovered potentially malformed switches.  Disable with '--allowmalformedswitch false'
                 * Unable to fully structure code
                 * Enabled aggressive block sorting
                 */
                public final Object invokeSuspend(Object $result) {
                    var13_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    block0 : switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            var2_3 = this.$snapshot.getDocumentChanges().iterator();
                            break;
                        }
                        case 1: {
                            isCustom = this.I$0;
                            standardCode = (String)this.L$9;
                            categoryClass = (String)this.L$8;
                            unit = (String)this.L$7;
                            subType = (String)this.L$6;
                            category = (String)this.L$5;
                            name = (String)this.L$4;
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v0 = $result;
                            break;
                        }
                        case 2: {
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            while (true) {
                                this.L$0 = var2_3;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)id);
                                this.label = 3;
                                v2 = FirebaseSyncManager.access$getDao$p(this.this$0).deleteStockForItem(id, (Continuation<? super Unit>)((Continuation)this));
                                if (v2 != var13_2) break block0;
                                return var13_2;
                            }
                        }
                        case 3: {
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v2 = $result;
                            break;
                        }
                        default: {
                            throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                        }
                    }
                    block11: while (var2_3.hasNext()) {
                        change = (DocumentChange)var2_3.next();
                        Intrinsics.checkNotNullExpressionValue((Object)change.getDocument(), (String)"getDocument(...)");
                        v3 = doc.getString("id");
                        if (v3 == null) {
                            v4 = doc.getId();
                            v3 = v4;
                            Intrinsics.checkNotNullExpressionValue((Object)v4, (String)"getId(...)");
                        }
                        id = v3;
                        switch (registerUnitListeners.itemsListener.1.WhenMappings.$EnumSwitchMapping$0[change.getType().ordinal()]) {
                            case 1: 
                            case 2: {
                                if (doc.getString("name") == null) continue block11;
                                v5 = doc.getString("serviceCategory");
                                if (v5 == null) {
                                    v5 = category = "\u041e\u0431\u0449\u0438\u0435";
                                }
                                if ((v6 = doc.getString("subType")) == null) {
                                    v6 = subType = "";
                                }
                                if ((v7 = doc.getString("unit")) == null) {
                                    v7 = unit = "\u0448\u0442.";
                                }
                                if ((v8 = doc.getString("categoryClass")) == null) {
                                    v8 = categoryClass = "\u041a\u0430\u0442. 1";
                                }
                                if ((v9 = doc.getString("standardCode")) == null) {
                                    v9 = "";
                                }
                                standardCode = v9;
                                v10 = doc.getBoolean("isCustom");
                                isCustom = (int)(v10 != null ? v10 : false);
                                this.L$0 = var2_3;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)id);
                                this.L$4 = SpillingKt.nullOutSpilledVariable((Object)name);
                                this.L$5 = SpillingKt.nullOutSpilledVariable((Object)category);
                                this.L$6 = SpillingKt.nullOutSpilledVariable((Object)subType);
                                this.L$7 = SpillingKt.nullOutSpilledVariable((Object)unit);
                                this.L$8 = SpillingKt.nullOutSpilledVariable((Object)categoryClass);
                                this.L$9 = SpillingKt.nullOutSpilledVariable((Object)standardCode);
                                this.I$0 = isCustom;
                                this.label = 1;
                                v0 = FirebaseSyncManager.access$getDao$p(this.this$0).insertItem(new InventoryItem(id, name, category, subType, unit, categoryClass, standardCode, isCustom != 0), (Continuation<? super Unit>)((Continuation)this));
                                if (v0 != var13_2) continue block11;
                                return var13_2;
                            }
                            case 3: {
                                this.L$0 = var2_3;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = id;
                                this.L$4 = null;
                                this.L$5 = null;
                                this.L$6 = null;
                                this.L$7 = null;
                                this.L$8 = null;
                                this.L$9 = null;
                                this.label = 2;
                                if ((v1 = FirebaseSyncManager.access$getDao$p(this.this$0).deleteItem(id, (Continuation<? super Unit>)((Continuation)this))) != var13_2) ** continue;
                                return var13_2;
                            }
                        }
                        throw new NoWhenBranchMatchedException();
                    }
                    return Unit.INSTANCE;
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)2, null);
        }
    }

    private static final void registerUnitListeners$lambda$4(FirebaseSyncManager this$0, QuerySnapshot snapshot, FirebaseFirestoreException error) {
        if (error != null) {
            Log.w((String)this$0.TAG, (String)"Stock listener error", (Throwable)((Throwable)error));
            return;
        }
        if (snapshot != null) {
            BuildersKt.launch$default((CoroutineScope)this$0.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(snapshot, this$0, null){
                Object L$0;
                Object L$1;
                Object L$2;
                Object L$3;
                Object L$4;
                int I$0;
                int I$1;
                int I$2;
                long J$0;
                int label;
                final /* synthetic */ QuerySnapshot $snapshot;
                final /* synthetic */ FirebaseSyncManager this$0;
                {
                    this.$snapshot = $snapshot;
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Recovered potentially malformed switches.  Disable with '--allowmalformedswitch false'
                 * Enabled aggressive block sorting
                 */
                public final Object invokeSuspend(Object $result) {
                    Object object;
                    Object object2;
                    DocumentChange change;
                    QueryDocumentSnapshot doc;
                    String pointId;
                    String itemId;
                    int quantity;
                    int incomeTotal;
                    int expenseTotal;
                    long lastUpdated;
                    Iterator iterator;
                    Object object3 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            iterator = this.$snapshot.getDocumentChanges().iterator();
                            break;
                        }
                        case 1: {
                            lastUpdated = this.J$0;
                            expenseTotal = this.I$2;
                            incomeTotal = this.I$1;
                            quantity = this.I$0;
                            itemId = (String)this.L$4;
                            pointId = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            iterator = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            object2 = $result;
                            break;
                        }
                        case 2: {
                            itemId = (String)this.L$4;
                            pointId = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            iterator = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            object = $result;
                            break;
                        }
                        default: {
                            throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                        }
                    }
                    block9: while (iterator.hasNext()) {
                        change = (DocumentChange)iterator.next();
                        Intrinsics.checkNotNullExpressionValue((Object)change.getDocument(), (String)"getDocument(...)");
                        if (doc.getString("pointId") == null || doc.getString("itemId") == null) continue;
                        switch (registerUnitListeners.stockListener.1.WhenMappings.$EnumSwitchMapping$0[change.getType().ordinal()]) {
                            case 1: 
                            case 2: {
                                Long l = doc.getLong("quantity");
                                quantity = (int)(l != null ? l : 0L);
                                Long l2 = doc.getLong("incomeTotal");
                                incomeTotal = (int)(l2 != null ? l2 : 0L);
                                Long l3 = doc.getLong("expenseTotal");
                                expenseTotal = (int)(l3 != null ? l3 : 0L);
                                Long l4 = doc.getLong("lastUpdated");
                                lastUpdated = l4 != null ? l4 : System.currentTimeMillis();
                                this.L$0 = iterator;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)pointId);
                                this.L$4 = SpillingKt.nullOutSpilledVariable((Object)itemId);
                                this.I$0 = quantity;
                                this.I$1 = incomeTotal;
                                this.I$2 = expenseTotal;
                                this.J$0 = lastUpdated;
                                this.label = 1;
                                object2 = FirebaseSyncManager.access$getDao$p(this.this$0).insertOrUpdateStock(new StockRecord(pointId, itemId, quantity, incomeTotal, expenseTotal, lastUpdated), (Continuation<? super Unit>)((Continuation)this));
                                if (object2 != object3) continue block9;
                                return object3;
                            }
                            case 3: {
                                this.L$0 = iterator;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)pointId);
                                this.L$4 = SpillingKt.nullOutSpilledVariable((Object)itemId);
                                this.label = 2;
                                object = FirebaseSyncManager.access$getDao$p(this.this$0).deleteStockRecord(pointId, itemId, (Continuation<? super Unit>)((Continuation)this));
                                if (object != object3) continue block9;
                                return object3;
                            }
                        }
                        throw new NoWhenBranchMatchedException();
                    }
                    return Unit.INSTANCE;
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)2, null);
        }
    }

    private static final void registerUnitListeners$lambda$5(FirebaseSyncManager this$0, Ref.BooleanRef $isFirstOpLoad, QuerySnapshot snapshot, FirebaseFirestoreException error) {
        if (error != null) {
            Log.w((String)this$0.TAG, (String)"Operations listener error", (Throwable)((Throwable)error));
            return;
        }
        if (snapshot != null) {
            boolean isInitial = $isFirstOpLoad.element;
            $isFirstOpLoad.element = false;
            BuildersKt.launch$default((CoroutineScope)this$0.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(snapshot, isInitial, this$0, null){
                Object L$0;
                Object L$1;
                Object L$2;
                Object L$3;
                Object L$4;
                Object L$5;
                Object L$6;
                Object L$7;
                Object L$8;
                Object L$9;
                Object L$10;
                Object L$11;
                Object L$12;
                Object L$13;
                long J$0;
                int label;
                final /* synthetic */ QuerySnapshot $snapshot;
                final /* synthetic */ boolean $isInitial;
                final /* synthetic */ FirebaseSyncManager this$0;
                {
                    this.$snapshot = $snapshot;
                    this.$isInitial = $isInitial;
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Recovered potentially malformed switches.  Disable with '--allowmalformedswitch false'
                 * Unable to fully structure code
                 * Enabled aggressive block sorting
                 * Enabled unnecessary exception pruning
                 * Enabled aggressive exception aggregation
                 */
                public final Object invokeSuspend(Object $result) {
                    var20_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            var2_3 = this.$snapshot.getDocumentChanges().iterator();
                            break;
                        }
                        case 1: {
                            opName = (String)this.L$6;
                            resp = (String)this.L$5;
                            typeStr = (String)this.L$4;
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v0 = $result;
                            ** GOTO lbl121
                        }
                        case 2: {
                            time = this.J$0;
                            op = (OperationRecord)this.L$13;
                            json = (String)this.L$12;
                            summary = (String)this.L$11;
                            comm = (String)this.L$10;
                            resp = (String)this.L$9;
                            docNum = (String)this.L$8;
                            toPoint = (String)this.L$7;
                            fromPoint = (String)this.L$6;
                            type = (OperationType)this.L$5;
                            typeStr = (String)this.L$4;
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            while (true) {
                                var8_10 = Unit.INSTANCE;
                                break;
                            }
                        }
                        case 3: {
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v2 = $result;
                            break;
                        }
                        default: {
                            throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                        }
                    }
                    block28: while (var2_3.hasNext()) {
                        change = (DocumentChange)var2_3.next();
                        Intrinsics.checkNotNullExpressionValue((Object)change.getDocument(), (String)"getDocument(...)");
                        v3 = doc.getString("id");
                        if (v3 == null) {
                            v4 = doc.getId();
                            v3 = v4;
                            Intrinsics.checkNotNullExpressionValue((Object)v4, (String)"getId(...)");
                        }
                        id = v3;
                        if (change.getType() == DocumentChange.Type.ADDED && !this.$isInitial && !doc.getMetadata().hasPendingWrites()) {
                            v5 = doc.getString("type");
                            if (v5 == null) {
                                v5 = typeStr = "";
                            }
                            if ((v6 = doc.getString("responsiblePerson")) == null) {
                                v6 = "";
                            }
                            resp = v6;
                            var9_11 = typeStr;
                            tmp = -1;
                            switch (var9_11.hashCode()) {
                                case 69972153: {
                                    if (!var9_11.equals("ISSUE")) break;
                                    tmp = 1;
                                    break;
                                }
                                case -2130930263: {
                                    if (!var9_11.equals("INCOME")) break;
                                    tmp = 2;
                                    break;
                                }
                                case 2063509483: {
                                    if (!var9_11.equals("TRANSFER")) break;
                                    tmp = 3;
                                    break;
                                }
                                case 106077535: {
                                    if (!var9_11.equals("EXPENDITURE")) break;
                                    tmp = 4;
                                    break;
                                }
                            }
                            switch (tmp) {
                                case 2: {
                                    v7 = "\u041f\u0440\u0438\u0445\u043e\u0434";
                                    break;
                                }
                                case 4: {
                                    v7 = "\u0421\u043f\u0438\u0441\u0430\u043d\u0438\u0435";
                                    break;
                                }
                                case 1: {
                                    v7 = "\u0412\u044b\u0434\u0430\u0447\u0430";
                                    break;
                                }
                                case 3: {
                                    v7 = "\u041f\u0435\u0440\u0435\u043c\u0435\u0449\u0435\u043d\u0438\u0435";
                                    break;
                                }
                                default: {
                                    v7 = "\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u044f";
                                }
                            }
                            opName = v7;
                            this.L$0 = var2_3;
                            this.L$1 = change;
                            this.L$2 = doc;
                            this.L$3 = id;
                            this.L$4 = SpillingKt.nullOutSpilledVariable((Object)typeStr);
                            this.L$5 = SpillingKt.nullOutSpilledVariable((Object)resp);
                            this.L$6 = SpillingKt.nullOutSpilledVariable((Object)opName);
                            this.L$7 = null;
                            this.L$8 = null;
                            this.L$9 = null;
                            this.L$10 = null;
                            this.L$11 = null;
                            this.L$12 = null;
                            this.L$13 = null;
                            this.label = 1;
                            v0 = FirebaseSyncManager.access$get_syncEvents$p(this.this$0).emit((Object)("\u2601\ufe0f \u041d\u043e\u0432\u0430\u044f \u043e\u043f\u0435\u0440\u0430\u0446\u0438\u044f \u043e\u0442 [" + resp + "]: " + opName), (Continuation)this);
                            if (v0 == var20_2) {
                                return var20_2;
                            }
                        }
lbl121:
                        // 5 sources

                        switch (registerUnitListeners.opListener.1.WhenMappings.$EnumSwitchMapping$0[change.getType().ordinal()]) {
                            case 1: 
                            case 2: {
                                try {
                                    v8 = doc.getString("type");
                                    if (v8 == null) {
                                        v8 = "INCOME";
                                    }
                                    typeStr = v8;
                                    try {
                                        var10_13 = OperationType.valueOf(typeStr);
                                    }
                                    catch (Exception <unused var>) {
                                        var10_13 = OperationType.INCOME;
                                    }
                                    type = var10_13;
                                    v9 = doc.getString("fromPointName");
                                    if (v9 == null) {
                                        v9 = fromPoint = "";
                                    }
                                    if ((v10 = doc.getString("toPointName")) == null) {
                                        v10 = toPoint = "";
                                    }
                                    if ((v11 = doc.getString("docNumber")) == null) {
                                        v11 = docNum = "";
                                    }
                                    if ((v12 = doc.getString("responsiblePerson")) == null) {
                                        v12 = resp = "";
                                    }
                                    if ((v13 = doc.getString("comment")) == null) {
                                        v13 = "";
                                    }
                                    comm = v13;
                                    v14 = doc.getLong("timestamp");
                                    time = v14 != null ? v14 : System.currentTimeMillis();
                                    v15 = doc.getString("itemsSummary");
                                    if (v15 == null) {
                                        v15 = summary = "";
                                    }
                                    if ((v16 = doc.getString("itemsJson")) == null) {
                                        v16 = "";
                                    }
                                    json = v16;
                                    op = new OperationRecord(id, type, fromPoint, toPoint, docNum, resp, comm, time, summary, json);
                                    this.L$0 = var2_3;
                                    this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                    this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                    this.L$3 = SpillingKt.nullOutSpilledVariable((Object)id);
                                    this.L$4 = SpillingKt.nullOutSpilledVariable((Object)typeStr);
                                    this.L$5 = SpillingKt.nullOutSpilledVariable((Object)type);
                                    this.L$6 = SpillingKt.nullOutSpilledVariable((Object)fromPoint);
                                    this.L$7 = SpillingKt.nullOutSpilledVariable((Object)toPoint);
                                    this.L$8 = SpillingKt.nullOutSpilledVariable((Object)docNum);
                                    this.L$9 = SpillingKt.nullOutSpilledVariable((Object)resp);
                                    this.L$10 = SpillingKt.nullOutSpilledVariable((Object)comm);
                                    this.L$11 = SpillingKt.nullOutSpilledVariable((Object)summary);
                                    this.L$12 = SpillingKt.nullOutSpilledVariable((Object)json);
                                    this.L$13 = SpillingKt.nullOutSpilledVariable((Object)op);
                                    this.J$0 = time;
                                    this.label = 2;
                                    if ((v1 = FirebaseSyncManager.access$getDao$p(this.this$0).insertOperation(op, (Continuation<? super Unit>)((Continuation)this))) != var20_2) ** continue;
                                }
                                catch (Exception e) {
                                    var8_10 = Boxing.boxInt((int)Log.e((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Error parsing operation doc", (Throwable)e));
                                }
                                return var20_2;
                                continue block28;
                            }
                            case 3: {
                                this.L$0 = var2_3;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)id);
                                this.L$4 = null;
                                this.L$5 = null;
                                this.L$6 = null;
                                this.L$7 = null;
                                this.L$8 = null;
                                this.L$9 = null;
                                this.L$10 = null;
                                this.L$11 = null;
                                this.L$12 = null;
                                this.L$13 = null;
                                this.label = 3;
                                v2 = FirebaseSyncManager.access$getDao$p(this.this$0).deleteOperation(id, (Continuation<? super Unit>)((Continuation)this));
                                if (v2 != var20_2) continue block28;
                                return var20_2;
                            }
                        }
                        throw new NoWhenBranchMatchedException();
                    }
                    var2_3 = (SyncState)FirebaseSyncManager.access$get_syncState$p(this.this$0).getValue();
                    var3_5 = System.currentTimeMillis();
                    FirebaseSyncManager.access$get_syncState$p(this.this$0).setValue((Object)SyncState.copy$default((SyncState)var2_3, false, var3_5, true, 0, "\u0421\u0438\u043d\u0445\u0440\u043e\u043d\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u043d\u043e \u0432 \u0440\u0435\u0436\u0438\u043c\u0435 \u043e\u043d\u043b\u0430\u0439\u043d", 8, null));
                    return Unit.INSTANCE;
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)2, null);
        }
    }

    private static final void registerUnitListeners$lambda$6(FirebaseSyncManager this$0, QuerySnapshot snapshot, FirebaseFirestoreException error) {
        if (error != null) {
            Log.w((String)this$0.TAG, (String)"Requisition listener error", (Throwable)((Throwable)error));
            return;
        }
        if (snapshot != null) {
            BuildersKt.launch$default((CoroutineScope)this$0.scope, (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(snapshot, this$0, null){
                Object L$0;
                Object L$1;
                Object L$2;
                Object L$3;
                Object L$4;
                Object L$5;
                Object L$6;
                Object L$7;
                Object L$8;
                Object L$9;
                Object L$10;
                Object L$11;
                long J$0;
                int label;
                final /* synthetic */ QuerySnapshot $snapshot;
                final /* synthetic */ FirebaseSyncManager this$0;
                {
                    this.$snapshot = $snapshot;
                    this.this$0 = $receiver;
                    super(2, $completion);
                }

                /*
                 * Recovered potentially malformed switches.  Disable with '--allowmalformedswitch false'
                 * Unable to fully structure code
                 * Enabled aggressive block sorting
                 * Enabled unnecessary exception pruning
                 * Enabled aggressive exception aggregation
                 */
                public final Object invokeSuspend(Object $result) {
                    var16_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            var2_3 = this.$snapshot.getDocumentChanges().iterator();
                            break;
                        }
                        case 1: {
                            time = this.J$0;
                            req = (RequisitionRequest)this.L$11;
                            json = (String)this.L$10;
                            summary = (String)this.L$9;
                            comm = (String)this.L$8;
                            status = (RequestStatus)this.L$7;
                            statusStr = (String)this.L$6;
                            applicant = (String)this.L$5;
                            pointName = (String)this.L$4;
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v0 = $result;
                            while (true) {
                                var6_7 = Unit.INSTANCE;
                                break;
                            }
                        }
                        case 2: {
                            id = (String)this.L$3;
                            doc = (QueryDocumentSnapshot)this.L$2;
                            change = (DocumentChange)this.L$1;
                            var2_3 = (Iterator)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
                            break;
                        }
                        default: {
                            throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                        }
                    }
                    block15: while (var2_3.hasNext()) {
                        change = (DocumentChange)var2_3.next();
                        Intrinsics.checkNotNullExpressionValue((Object)change.getDocument(), (String)"getDocument(...)");
                        v2 = doc.getString("id");
                        if (v2 == null) {
                            v3 = doc.getId();
                            v2 = v3;
                            Intrinsics.checkNotNullExpressionValue((Object)v3, (String)"getId(...)");
                        }
                        id = v2;
                        switch (registerUnitListeners.reqListener.1.WhenMappings.$EnumSwitchMapping$0[change.getType().ordinal()]) {
                            case 1: 
                            case 2: {
                                try {
                                    v4 = doc.getString("pointName");
                                    if (v4 == null) {
                                        v4 = pointName = "";
                                    }
                                    if ((v5 = doc.getString("applicantName")) == null) {
                                        v5 = applicant = "";
                                    }
                                    if ((v6 = doc.getString("status")) == null) {
                                        v6 = "PENDING";
                                    }
                                    statusStr = v6;
                                    try {
                                        var10_12 = RequestStatus.valueOf(statusStr);
                                    }
                                    catch (Exception <unused var>) {
                                        var10_12 = RequestStatus.PENDING;
                                    }
                                    status = var10_12;
                                    v7 = doc.getString("comment");
                                    if (v7 == null) {
                                        v7 = "";
                                    }
                                    comm = v7;
                                    v8 = doc.getLong("timestamp");
                                    time = v8 != null ? v8 : System.currentTimeMillis();
                                    v9 = doc.getString("itemsSummary");
                                    if (v9 == null) {
                                        v9 = summary = "";
                                    }
                                    if ((v10 = doc.getString("itemsJson")) == null) {
                                        v10 = "";
                                    }
                                    json = v10;
                                    req = new RequisitionRequest(id, pointName, applicant, status, comm, time, summary, json);
                                    this.L$0 = var2_3;
                                    this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                    this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                    this.L$3 = SpillingKt.nullOutSpilledVariable((Object)id);
                                    this.L$4 = SpillingKt.nullOutSpilledVariable((Object)pointName);
                                    this.L$5 = SpillingKt.nullOutSpilledVariable((Object)applicant);
                                    this.L$6 = SpillingKt.nullOutSpilledVariable((Object)statusStr);
                                    this.L$7 = SpillingKt.nullOutSpilledVariable((Object)status);
                                    this.L$8 = SpillingKt.nullOutSpilledVariable((Object)comm);
                                    this.L$9 = SpillingKt.nullOutSpilledVariable((Object)summary);
                                    this.L$10 = SpillingKt.nullOutSpilledVariable((Object)json);
                                    this.L$11 = SpillingKt.nullOutSpilledVariable((Object)req);
                                    this.J$0 = time;
                                    this.label = 1;
                                    if ((v0 = FirebaseSyncManager.access$getDao$p(this.this$0).insertRequisition(req, (Continuation<? super Unit>)((Continuation)this))) != var16_2) ** continue;
                                }
                                catch (Exception e) {
                                    var6_7 = Boxing.boxInt((int)Log.e((String)FirebaseSyncManager.access$getTAG$p(this.this$0), (String)"Error parsing requisition doc", (Throwable)e));
                                }
                                return var16_2;
                                continue block15;
                            }
                            case 3: {
                                this.L$0 = var2_3;
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)change);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)doc);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)id);
                                this.L$4 = null;
                                this.L$5 = null;
                                this.L$6 = null;
                                this.L$7 = null;
                                this.L$8 = null;
                                this.L$9 = null;
                                this.L$10 = null;
                                this.L$11 = null;
                                this.label = 2;
                                v1 = FirebaseSyncManager.access$getDao$p(this.this$0).deleteRequisition(id, (Continuation<? super Unit>)((Continuation)this));
                                if (v1 != var16_2) continue block15;
                                return var16_2;
                            }
                        }
                        throw new NoWhenBranchMatchedException();
                    }
                    return Unit.INSTANCE;
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)2, null);
        }
    }

    private static final void registerUnitListeners$lambda$8(FirebaseSyncManager this$0, QuerySnapshot snapshot, FirebaseFirestoreException firebaseFirestoreException) {
        if (snapshot != null) {
            int n;
            long now = System.currentTimeMillis();
            List list = snapshot.getDocuments();
            Intrinsics.checkNotNullExpressionValue((Object)list, (String)"getDocuments(...)");
            Iterable $this$count$iv = list;
            boolean $i$f$count = false;
            if ($this$count$iv instanceof Collection && ((Collection)$this$count$iv).isEmpty()) {
                n = 0;
            } else {
                int count$iv = 0;
                for (Object element$iv : $this$count$iv) {
                    DocumentSnapshot doc = (DocumentSnapshot)element$iv;
                    boolean bl = false;
                    Long l = doc.getLong("timestampMillis");
                    long lastSeen = l != null ? l : 0L;
                    if (!(now - lastSeen < 900000L) || ++count$iv >= 0) continue;
                    CollectionsKt.throwCountOverflow();
                }
                n = count$iv;
            }
            int activeCount = n;
            SyncState syncState = (SyncState)this$0._syncState.getValue();
            int n2 = activeCount > 0 ? activeCount : 1;
            this$0._syncState.setValue((Object)SyncState.copy$default(syncState, false, 0L, true, n2, null, 19, null));
        }
    }

    public static final /* synthetic */ FirebaseFirestore access$getFirestore(FirebaseSyncManager $this) {
        return $this.getFirestore();
    }

    public static final /* synthetic */ MutableStateFlow access$get_syncState$p(FirebaseSyncManager $this) {
        return $this._syncState;
    }

    public static final /* synthetic */ String access$getTAG$p(FirebaseSyncManager $this) {
        return $this.TAG;
    }

    public static final /* synthetic */ KapterkaDao access$getDao$p(FirebaseSyncManager $this) {
        return $this.dao;
    }

    public static final /* synthetic */ MutableSharedFlow access$get_syncEvents$p(FirebaseSyncManager $this) {
        return $this._syncEvents;
    }
}
