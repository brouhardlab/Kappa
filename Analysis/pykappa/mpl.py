import matplotlib.pyplot as plt


def set_ax_style(ax, font="DejaVu Sans", font_size_ratio=1,
                 axes_color="#231f20ff", x_axis_grid=False,
                 y_axis_grid=True, tick_width=None):
    """Set a nice style to your Matplotlib figure.

    Parameters:
        ax: Axes
            The Matplotlib axes.
        font: str
            Font family.
        font_size_ratio: float
            Increase of decrease the font size.
        axes_color: str
            Colors of the axes lines and thicks.
        x_axis_grid: bool
            Add X axis grid.
        y_axis_grid: bool
            Add Y axis grid.
    """
    fig = ax.figure

    w_inches, _ = ax.figure.get_size_inches()
    w = w_inches * fig.dpi

    tick_length = w / 100
    if not tick_width:
        tick_width = w / 300
    font_size = int(w * font_size_ratio / 35)

    ax.spines['left'].set_linewidth(tick_width)
    ax.spines['bottom'].set_linewidth(tick_width)
    ax.spines['left'].set_color(axes_color)
    ax.spines['bottom'].set_color(axes_color)
    ax.spines['right'].set_visible(False)
    ax.spines['top'].set_visible(False)

    ax.xaxis.set_tick_params(direction='in', color=axes_color, labelsize=font_size,
                             length=tick_length, width=tick_width)
    ax.yaxis.set_tick_params(direction='in', color=axes_color, labelsize=font_size,
                             length=tick_length, width=tick_width)

    ax.yaxis.set_ticks_position('left')
    ax.xaxis.set_ticks_position('bottom')

    for label in ax.get_xticklabels():
        label.set_family(font)
    for label in ax.get_yticklabels():
        label.set_family(font)

    ax.xaxis.label.set_family(font)
    ax.yaxis.label.set_family(font)
    ax.xaxis.label.set_size(font_size)
    ax.yaxis.label.set_size(font_size)

    if x_axis_grid:
        ax.xaxis.grid(True, color=axes_color, linestyle='-', alpha=0.2, lw=4)
    else:
        ax.xaxis.grid(False)
    if y_axis_grid:
        ax.yaxis.grid(True, color=axes_color, linestyle='-', alpha=0.2, lw=4)
    else:
        ax.yaxis.grid(False)


def display_system_fonts():
    import matplotlib.font_manager
    from IPython.core.display import HTML

    def make_html(fontname):
        return "<p>{font}: <span style='font-family:{font}; font-size: 24px;'>{font}</p>".format(font=fontname)
    code = "\n".join([make_html(font) for font in sorted(set([f.name for f in matplotlib.font_manager.fontManager.ttflist]))])
    return HTML("<div style='column-count: 3;'>{}</div>".format(code))


def plot_error(data, metric_label,
               x_ticks, y_ticks,
               x_label, y_label,
               x_lim=None, y_lim=None,
               color='black', base_size=16,
               font_size_ratio=2, labels_spacing=0.02):

    #fig, ax = plt.subplots(figsize=(base_size, int(base_size / 1.3)))
    fig, ax = plt.subplots(figsize=(base_size, base_size))

    df = data.groupby("feature").mean()[metric_label]
    df_err = data.groupby("feature").sem()[metric_label]

    ax.plot(df.index, df.values, color=color, lw=4)
    ax.errorbar(df.index, df.values, df_err.values,
                marker='o', markersize=15, linestyle='none',
                elinewidth=6, color=color)

    ax.set_xticks(x_ticks)
    ax.set_yticks(y_ticks)
    ax.set_xlabel(x_label)
    ax.set_ylabel(y_label)

    if labels_spacing:
        for obj in ax.xaxis.get_majorticklabels():
            obj.set_y(-labels_spacing)
        for obj in ax.yaxis.get_majorticklabels():
            obj.set_x(-labels_spacing)

    if x_lim:
        ax.set_xlim(*x_lim)
    if y_lim:
        ax.set_ylim(*y_lim)

    kwargs = dict(font="DejaVu Sans", font_size_ratio=2, axes_color="#231f20ff",
                  x_axis_grid=False, y_axis_grid=True, tick_width=5)
    set_ax_style(ax, **kwargs)

    return fig