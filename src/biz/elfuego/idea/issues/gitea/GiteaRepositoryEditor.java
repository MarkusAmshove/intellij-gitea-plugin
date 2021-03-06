/*
 * Copyright © 2018 by elfuego.biz
 */
package biz.elfuego.idea.issues.gitea;

import biz.elfuego.idea.issues.gitea.model.GiteaProject;
import biz.elfuego.idea.issues.gitea.util.Consts.ProjectFilter;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.tasks.impl.TaskUiUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Consumer;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author Roman Pedchenko <elfuego@elfuego.biz>
 * @date 2018.06.30
 */
public class GiteaRepositoryEditor extends BaseRepositoryEditor<GiteaRepository> {
    private JBLabel projectLabel;
    private ComboBox projectBox;

    private JBLabel filterLabel;
    private ComboBox<ProjectFilter> filterBox;

    GiteaRepositoryEditor(GiteaRepository repository, Project project, Consumer<GiteaRepository> consumer) {
        super(project, repository, consumer);

        filterBox.setSelectedItem(myRepository.getProjectFilter());
        projectBox.setSelectedItem(myRepository.getSelectedProject());
        installListener(filterBox);
        installListener(projectBox);

        UIUtil.invokeLaterIfNeeded(this::initialize);
    }

    private void initialize() {
        if (myRepository.isConfigured()) {
            new FetchProjectsTask().queue();
        }
    }

    @Nullable
    @Override
    protected JComponent createCustomPanel() {
        filterBox = new ComboBox<>(ProjectFilter.values(), 300);
        filterBox.addActionListener(e -> new FetchProjectsTask().queue());
        filterLabel = new JBLabel("Project filter:", SwingConstants.RIGHT);
        filterLabel.setLabelFor(filterBox);

        projectBox = new ComboBox(300);
        projectBox.setRenderer(new TaskUiUtil.SimpleComboBoxRenderer("Set URL, username, and password"));
        projectLabel = new JBLabel("Project:", SwingConstants.RIGHT);
        projectLabel.setLabelFor(projectBox);

        return new FormBuilder().setAlignLabelOnRight(true)
                .addLabeledComponent(filterLabel, filterBox)
                .addLabeledComponent(projectLabel, projectBox)
                .getPanel();
    }

    @Override
    public void setAnchor(@Nullable JComponent anchor) {
        super.setAnchor(anchor);
        filterLabel.setAnchor(anchor);
        projectLabel.setAnchor(anchor);
    }

    @Override
    protected void afterTestConnection(boolean connectionSuccessful) {
        if (connectionSuccessful) {
            new FetchProjectsTask().queue();
        }
    }

    @Override
    public void apply() {
        super.apply();
        myRepository.setProjectFilter((ProjectFilter) filterBox.getSelectedItem());
        myRepository.setSelectedProject((GiteaProject) projectBox.getSelectedItem());
        myTestButton.setEnabled(myRepository.isConfigured());
    }

    private class FetchProjectsTask extends TaskUiUtil.ComboBoxUpdater<GiteaProject> {
        private FetchProjectsTask() {
            super(GiteaRepositoryEditor.this.myProject, "Downloading Gitea projects...", projectBox);
        }

        @Override
        public GiteaProject getExtraItem() {
            return GiteaProject.UNSPECIFIED_PROJECT;
        }

        @Nullable
        @Override
        public GiteaProject getSelectedItem() {
            return myRepository.getSelectedProject();
        }

        @NotNull
        @Override
        protected List<GiteaProject> fetch(@NotNull ProgressIndicator indicator) throws Exception {
            return myRepository.getProjectList((ProjectFilter) filterBox.getSelectedItem());
        }
    }
}
