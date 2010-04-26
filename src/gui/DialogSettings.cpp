// -*- C++ -*- generated by wxGlade 0.6.3

#include "DialogSettings.h"

// begin wxGlade: ::extracode

// end wxGlade


DialogSettings::DialogSettings(wxWindow* parent, int id, const wxString& title, const wxPoint& pos, const wxSize& size, long style):
    wxDialog(parent, id, title, pos, size, wxDEFAULT_DIALOG_STYLE)
{
    // begin wxGlade: DialogSettings::DialogSettings
    notebook_settings = new wxNotebook(this, wxID_ANY, wxDefaultPosition, wxDefaultSize, 0);
    notebook_settings_pane_logging = new wxPanel(notebook_settings, wxID_ANY);
    notebook_settings_pane_conn = new wxPanel(notebook_settings, wxID_ANY);
    sizer_quality_staticbox = new wxStaticBox(notebook_settings_pane_conn, -1, _("JPEG Quality"));
    sizer_fastrequest_staticbox = new wxStaticBox(notebook_settings_pane_conn, -1, _("FastRequest"));
    sizer_multicast_staticbox = new wxStaticBox(notebook_settings_pane_conn, -1, _("MulticastVNC"));
    sizer_compresslevel_staticbox = new wxStaticBox(notebook_settings_pane_conn, -1, _("Compression Level"));
    label_compresslevel = new wxStaticText(notebook_settings_pane_conn, wxID_ANY, _("Use specified compression level for \"Tight\" and \"Zlib\" encodings:"));
    slider_compresslevel = new wxSlider(notebook_settings_pane_conn, wxID_ANY, 0, 0, 9, wxDefaultPosition, wxDefaultSize, wxSL_HORIZONTAL|wxSL_AUTOTICKS|wxSL_LABELS);
    label_quality = new wxStaticText(notebook_settings_pane_conn, wxID_ANY, _("Use the specified JPEG quality level for \"Tight\" encoding:"));
    slider_quality = new wxSlider(notebook_settings_pane_conn, wxID_ANY, 0, 0, 9, wxDefaultPosition, wxDefaultSize, wxSL_HORIZONTAL|wxSL_AUTOTICKS|wxSL_LABELS);
    checkbox_fastrequest = new wxCheckBox(notebook_settings_pane_conn, wxID_ANY, _("Use FastRequest"));
    label_fastrequest = new wxStaticText(notebook_settings_pane_conn, wxID_ANY, _("Continously request updates at the specified milisecond interval:"));
    slider_fastrequest = new wxSlider(notebook_settings_pane_conn, wxID_ANY, 0, 1, 100, wxDefaultPosition, wxDefaultSize, wxSL_HORIZONTAL|wxSL_AUTOTICKS|wxSL_LABELS);
    checkbox_multicast = new wxCheckBox(notebook_settings_pane_conn, wxID_ANY, _("Use MulticastVNC"));
    label_recvbuf = new wxStaticText(notebook_settings_pane_conn, wxID_ANY, _("Receive Buffer Size (kB):"));
    slider_recvbuf = new wxSlider(notebook_settings_pane_conn, wxID_ANY, 0, 65, 9750, wxDefaultPosition, wxDefaultSize, wxSL_HORIZONTAL|wxSL_LABELS);
    checkbox_logfile = new wxCheckBox(notebook_settings_pane_logging, wxID_ANY, _("Write VNC log to logfile (MultiVNC.log)"));
    checkbox_stats_save = new wxCheckBox(notebook_settings_pane_logging, wxID_ANY, _("Autosave statistics on close"));

    set_properties();
    do_layout();
    // end wxGlade
}


void DialogSettings::set_properties()
{
    // begin wxGlade: DialogSettings::set_properties
    slider_compresslevel->SetToolTip(_("Use specified compression level (0..9) for \"tight\" and \"zlib\" encodings. Level 1 uses minimum of CPU time and achieves weak compression ratios, while level 9 offers best compression but is slow in terms of CPU time consumption on the server side. Use high levels with very slow network connections, and low levels when working over high-speed LANs."));
    slider_quality->SetToolTip(_("Use the specified JPEG quality level (0..9) for the \"Tight\" encoding. Quality level 0 denotes bad image quality but very impressive compression ratios, while level 9 offers very good image quality at lower compression ratios. Note that the \"tight\" encoder uses JPEG to encode only those screen areas that look suitable for lossy compression, so quality level 0 does not always mean unacceptable image quality."));
    slider_fastrequest->SetToolTip(_("Continously ask the server for updates instead of just asking after each received server message. Use this on high latency links."));
    slider_recvbuf->SetToolTip(_("Set the multicast receive buffer size. Increasing the value may help against packet loss. Note that depending on your OS, you may not always get the requested value. View the log to see what happened."));
    // end wxGlade
}


void DialogSettings::do_layout()
{
    // begin wxGlade: DialogSettings::do_layout
    wxBoxSizer* sizer_top = new wxBoxSizer(wxVERTICAL);
    wxBoxSizer* sizer_logging = new wxBoxSizer(wxVERTICAL);
    wxBoxSizer* sizer_conn = new wxBoxSizer(wxVERTICAL);
    wxStaticBoxSizer* sizer_multicast = new wxStaticBoxSizer(sizer_multicast_staticbox, wxVERTICAL);
    wxStaticBoxSizer* sizer_fastrequest = new wxStaticBoxSizer(sizer_fastrequest_staticbox, wxVERTICAL);
    wxStaticBoxSizer* sizer_quality = new wxStaticBoxSizer(sizer_quality_staticbox, wxVERTICAL);
    wxStaticBoxSizer* sizer_compresslevel = new wxStaticBoxSizer(sizer_compresslevel_staticbox, wxVERTICAL);
    sizer_compresslevel->Add(label_compresslevel, 0, wxALL|wxADJUST_MINSIZE, 3);
    sizer_compresslevel->Add(slider_compresslevel, 0, wxALL|wxEXPAND|wxADJUST_MINSIZE, 3);
    sizer_conn->Add(sizer_compresslevel, 1, wxALL|wxEXPAND, 3);
    sizer_quality->Add(label_quality, 0, wxALL|wxADJUST_MINSIZE, 3);
    sizer_quality->Add(slider_quality, 0, wxALL|wxEXPAND|wxADJUST_MINSIZE, 3);
    sizer_conn->Add(sizer_quality, 1, wxALL|wxEXPAND, 3);
    sizer_fastrequest->Add(checkbox_fastrequest, 0, wxALL|wxEXPAND|wxADJUST_MINSIZE, 3);
    sizer_fastrequest->Add(label_fastrequest, 0, wxALL|wxADJUST_MINSIZE, 3);
    sizer_fastrequest->Add(slider_fastrequest, 0, wxALL|wxEXPAND|wxADJUST_MINSIZE, 3);
    sizer_conn->Add(sizer_fastrequest, 1, wxALL|wxEXPAND, 3);
    sizer_multicast->Add(checkbox_multicast, 0, wxALL|wxEXPAND|wxADJUST_MINSIZE, 3);
    sizer_multicast->Add(label_recvbuf, 0, wxALL|wxADJUST_MINSIZE, 3);
    sizer_multicast->Add(slider_recvbuf, 0, wxALL|wxEXPAND|wxADJUST_MINSIZE, 3);
    sizer_conn->Add(sizer_multicast, 1, wxALL|wxEXPAND, 3);
    notebook_settings_pane_conn->SetSizer(sizer_conn);
    sizer_logging->Add(checkbox_logfile, 0, wxALL|wxADJUST_MINSIZE, 6);
    sizer_logging->Add(checkbox_stats_save, 0, wxALL|wxADJUST_MINSIZE, 6);
    notebook_settings_pane_logging->SetSizer(sizer_logging);
    notebook_settings->AddPage(notebook_settings_pane_conn, _("Connections"));
    notebook_settings->AddPage(notebook_settings_pane_logging, _("Logging"));
    sizer_top->Add(notebook_settings, 1, wxALL|wxEXPAND, 3);
    SetSizer(sizer_top);
    sizer_top->Fit(this);
    Layout();
    // end wxGlade
}

