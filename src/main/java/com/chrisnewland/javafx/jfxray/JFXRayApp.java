/*
 * Copyright (c) 2013 Chris Newland. All rights reserved.
 * Licensed under https://github.com/chriswhocodes/jitwatch/blob/master/LICENSE-BSD
 * http://www.chrisnewland.com/jitwatch
 */
package com.chrisnewland.javafx.jfxray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.TimelineBuilder;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class JFXRayApp extends Application
{
    private Stage stage;

  
    // Called by JFX
    public JFXRayApp()
    {
    }

    public JFXRayApp(String[] args)
    {
        launch(args);
    }

    public void openTreeAtMember(IMetaMember member)
    {
        List<String> path = member.getTreePath();

        // would be better to identify open nodes and close?
        clearAndRefresh();

        TreeItem<Object> curNode = classTree.getRootItem();

        StringBuilder builtPath = new StringBuilder();

        int pathLength = path.size();
        int pos = 0;

        int rowsAbove = 0;

        boolean found = false;

        for (String part : path)
        {
            builtPath.append(part);

            String matching;

            found = false;

            if (pos++ == pathLength - 1)
            {
                matching = part;
            }
            else
            {
                matching = builtPath.toString();
            }

            for (TreeItem<Object> node : curNode.getChildren())
            {
                rowsAbove++;

                String nodeText = node.getValue().toString();

                if (matching.equals(nodeText))
                {
                    builtPath.append('.');
                    curNode = node;
                    curNode.setExpanded(true);
                    classTree.select(curNode);
                    found = true;
                    break;
                }
            }
        }

        if (found)
        {
            classTree.scrollTo(rowsAbove);
            classMemberList.selectMember(member);
        }
    }

    void openSource(IMetaMember member)
    {
        MetaClass methodClass = member.getMetaClass();

        String fqName = methodClass.getFullyQualifiedName();

        fqName = fqName.replace(".", "/") + ".java";

        String source = ResourceLoader.getSource(config.getSourceLocations(), fqName);

        TextViewerStage tvs = null;
        String title = "Source code for " + fqName;

        for (Stage s : openPopupStages)
        {
            if (s instanceof TextViewerStage && title.equals(s.getTitle()))
            {
                tvs = (TextViewerStage) s;
                break;
            }
        }

        if (tvs == null)
        {
            tvs = new TextViewerStage(JITWatchUI.this, title, source, true);
            tvs.show();
            openPopupStages.add(tvs);
        }

        tvs.requestFocus();

        tvs.jumpTo(member.getSignatureRegEx());
    }

    void openBytecode(IMetaMember member)
    {
        String searchMethod = member.getSignatureForBytecode();

        MetaClass methodClass = member.getMetaClass();

        Map<String, String> bytecodeCache = methodClass.getBytecodeCache(config.getClassLocations());

        String bc = bytecodeCache.get(searchMethod);

        TextViewerStage tvs = new TextViewerStage(JITWatchUI.this, "Bytecode for " + member.toString(), bc, false);
        tvs.show();

        openPopupStages.add(tvs);
    }

    void openNativeCode(IMetaMember member)
    {
        String nativeCode = member.getNativeCode();
        TextViewerStage tvs = new TextViewerStage(JITWatchUI.this, "Native code for " + member.toString(), nativeCode, false);
        tvs.show();

        openPopupStages.add(tvs);
    }
    
    void openTextViewer(String title, String content)
    {
        TextViewerStage tvs = new TextViewerStage(JITWatchUI.this, title, content, false);
        tvs.show();
        openPopupStages.add(tvs);
    }

    private void chooseHotSpotFile()
    {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose HotSpot log file");
        String curDir = System.getProperty("user.dir");
        File dirFile = new File(curDir);
        fc.setInitialDirectory(dirFile);

        File result = fc.showOpenDialog(stage);

        if (result != null)
        {
            watchFile = result;
            log("Selected file: " + watchFile.getAbsolutePath());
            log("Click Start button to process or tail the file");
            updateButtons();

            refreshLog();
        }
    }

    void showMemberInfo(IMetaMember member)
    {
        memberAttrList.clear();

        if (member == null)
        {
            return;
        }

        selectedMember = member;

        List<String> queuedAttrKeys = member.getQueuedAttributes();

        for (String key : queuedAttrKeys)
        {
            memberAttrList.add(new AttributeTableRow("Queued", key, member.getQueuedAttribute(key)));
        }

        List<String> compiledAttrKeys = member.getCompiledAttributes();

        for (String key : compiledAttrKeys)
        {
            memberAttrList.add(new AttributeTableRow("Compiled", key, member.getCompiledAttribute(key)));
        }
    }

    private void refresh()
    {
        if (repaintTree)
        {
            repaintTree = false;
            classTree.showTree();
        }

        if (timeLineStage != null)
        {
            timeLineStage.redraw();
        }

        if (statsStage != null)
        {
            statsStage.redraw();
        }

        if (histoStage != null)
        {
            histoStage.redraw();
        }

        if (topListStage != null)
        {
            topListStage.redraw();
        }

        if (logBuffer.length() > 0)
        {
            refreshLog();
        }

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        long megabyte = 1024 * 1024;

        String heapString = "Heap: " + (usedMemory / megabyte) + "/" + (totalMemory / megabyte) + "M";

        lblHeap.setText(heapString);

        btnErrorLog.setText("Errors (" + errorCount + ")");
    }

    private void refreshLog()
    {
        textAreaLog.appendText(logBuffer.toString());
        logBuffer.delete(0, logBuffer.length());
    }

    public IMetaMember getSelectedMember()
    {
        return selectedMember;
    }

    void clearAndRefresh()
    {
        selectedMember = null;
        classTree.clear();
        classTree.showTree();
    }

    public void handleStageClosed(Stage stage)
    {
        openPopupStages.remove(stage);

        if (stage instanceof TimeLineStage)
        {
            btnTimeLine.setDisable(false);
            timeLineStage = null;
        }
        else if (stage instanceof StatsStage)
        {
            btnStats.setDisable(false);
            statsStage = null;
        }
        else if (stage instanceof HistoStage)
        {
            btnHisto.setDisable(false);
            histoStage = null;
        }
        else if (stage instanceof ConfigStage)
        {
            btnConfigure.setDisable(false);
            configStage = null;
        }
        else if (stage instanceof TopListStage)
        {
            btnTopList.setDisable(false);
            topListStage = null;
        }
    }

    @Override
    public void handleJITEvent(JITEvent event)
    {
        log(event.toString());
        repaintTree = true;
    }

    @Override
    public void handleLogEntry(String entry)
    {
        log(entry);
    }

    @Override
    public void handleErrorEntry(String entry)
    {
        errorLog.append(entry).append("\n");
        errorCount++;
    }

    private void log(final String entry)
    {
        logBuffer.append(entry + "\n");
    }

    void refreshSelectedTreeNode(MetaClass metaClass)
    {
        classMemberList.clearClassMembers();

        showMemberInfo(null);

        if (metaClass == null)
        {
            // nothing selected
            return;
        }

        classMemberList.setMetaClass(metaClass);
    }

    public PackageManager getPackageManager()
    {
        return model.getPackageManager();
    }
    
    public Journal getJournal(String id)
    {
    	return model.getJournal(id);
    }
}